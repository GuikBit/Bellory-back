package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.push.PushSubscriptionRequestDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.push.PushNotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/push")
@Tag(name = "Push Notifications", description = "Gerenciamento de Web Push Notifications")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;

    @Value("${vapid.public-key:}")
    private String vapidPublicKey;

    public PushNotificationController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
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
