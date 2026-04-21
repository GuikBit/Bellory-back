package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.martijndwars.webpush.PushService;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.push.PushSubscriptionRequestDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.push.PushSubscription;
import org.exemplo.bellory.model.repository.push.PushSubscriptionRepository;
import org.exemplo.bellory.service.push.PushNotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/push")
@Tag(name = "Push Notifications", description = "Gerenciamento de Web Push Notifications")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushService pushService;

    @Value("${vapid.public-key:}")
    private String vapidPublicKey;

    public PushNotificationController(PushNotificationService pushNotificationService,
                                      PushSubscriptionRepository pushSubscriptionRepository,
                                      @Nullable PushService pushService) {
        this.pushNotificationService = pushNotificationService;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.pushService = pushService;
    }

    @Operation(summary = "Obter chave publica VAPID")
    @GetMapping("/vapid-public-key")
    public ResponseEntity<ResponseAPI<Map<String, String>>> getVapidPublicKey() {
        return ResponseEntity.ok(ResponseAPI.<Map<String, String>>builder()
                .success(true)
                .message("Chave publica VAPID")
                .dados(Map.of("publicKey", vapidPublicKey))
                .build());
    }

    @Operation(summary = "Registrar subscription de push")
    @PostMapping("/subscribe")
    public ResponseEntity<ResponseAPI<Void>> subscribe(
            @RequestBody PushSubscriptionRequestDTO dto,
            HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            pushNotificationService.subscribe(dto, userAgent);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Subscription registrada com sucesso")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao registrar subscription: " + e.getMessage())
                            .errorCode(400)
                            .build());
        }
    }

    @Operation(summary = "Diagnostico do sistema de push", description = "Verifica VAPID, subscriptions do usuario logado e estado geral")
    @GetMapping("/diagnostico")
    public ResponseEntity<ResponseAPI<Map<String, Object>>> diagnostico() {
        Map<String, Object> diag = new LinkedHashMap<>();

        // 1. VAPID configurado?
        boolean vapidConfigurado = vapidPublicKey != null && !vapidPublicKey.isBlank();
        diag.put("vapidPublicKeyConfigurada", vapidConfigurado);
        diag.put("vapidPublicKeyPreview", vapidConfigurado ? vapidPublicKey.substring(0, Math.min(20, vapidPublicKey.length())) + "..." : "NAO CONFIGURADA");

        // 2. PushService bean existe?
        diag.put("pushServiceAtivo", pushService != null);

        // 3. Contexto do usuario
        Long userId = TenantContext.getCurrentUserId();
        String userRole = TenantContext.getCurrentRole();
        Long orgId = TenantContext.getCurrentOrganizacaoId();
        diag.put("userId", userId);
        diag.put("userRole", userRole);
        diag.put("organizacaoId", orgId);

        // 4. Subscriptions do usuario logado
        if (userId != null && userRole != null && orgId != null) {
            List<PushSubscription> minhasSubs = pushSubscriptionRepository
                    .findAllByUserIdAndUserRoleAndOrganizacao_Id(userId, userRole, orgId);
            diag.put("subscriptionsDoUsuario", minhasSubs.size());
            diag.put("subscriptionEndpoints", minhasSubs.stream()
                    .map(s -> s.getEndpoint().substring(0, Math.min(60, s.getEndpoint().length())) + "...")
                    .toList());
        } else {
            diag.put("subscriptionsDoUsuario", "N/A - contexto incompleto");
        }

        // 5. Total de subscriptions da organizacao
        if (orgId != null) {
            List<PushSubscription> todasSubs = pushSubscriptionRepository.findAllByOrganizacao_Id(orgId);
            diag.put("totalSubscriptionsOrganizacao", todasSubs.size());
        }

        // 6. Veredicto
        List<String> problemas = new java.util.ArrayList<>();
        if (!vapidConfigurado) problemas.add("VAPID public key nao configurada");
        if (pushService == null) problemas.add("PushService bean e NULL - chaves VAPID invalidas ou ausentes");
        if (userId != null && userRole != null && orgId != null) {
            List<PushSubscription> subs = pushSubscriptionRepository
                    .findAllByUserIdAndUserRoleAndOrganizacao_Id(userId, userRole, orgId);
            if (subs.isEmpty()) problemas.add("Nenhuma subscription registrada para este usuario - frontend precisa chamar POST /api/v1/push/subscribe");
        }
        diag.put("problemas", problemas);
        diag.put("status", problemas.isEmpty() ? "OK" : "PROBLEMAS ENCONTRADOS");

        return ResponseEntity.ok(ResponseAPI.<Map<String, Object>>builder()
                .success(true)
                .message("Diagnostico do push")
                .dados(diag)
                .build());
    }

    @Operation(summary = "Remover subscription de push")
    @PostMapping("/unsubscribe")
    public ResponseEntity<ResponseAPI<Void>> unsubscribe(@RequestBody Map<String, String> body) {
        try {
            String endpoint = body.get("endpoint");
            if (endpoint == null || endpoint.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<Void>builder()
                                .success(false)
                                .message("Endpoint e obrigatorio")
                                .errorCode(400)
                                .build());
            }
            pushNotificationService.unsubscribe(endpoint);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Subscription removida com sucesso")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao remover subscription: " + e.getMessage())
                            .errorCode(400)
                            .build());
        }
    }
}
