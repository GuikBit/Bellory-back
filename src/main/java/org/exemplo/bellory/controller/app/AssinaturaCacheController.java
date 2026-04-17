package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.client.payment.PaymentApiClient;
import org.exemplo.bellory.client.payment.dto.ChargeResponse;
import org.exemplo.bellory.client.payment.dto.PlanResponse;
import org.exemplo.bellory.client.payment.dto.SubscriptionResponse;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.exception.PaymentApiException;
import org.exemplo.bellory.model.dto.assinatura.AssinaturaCompletaDTO;
import org.exemplo.bellory.model.dto.assinatura.AssinaturaStatusDTO;
import org.exemplo.bellory.model.entity.assinatura.Assinatura;
import org.exemplo.bellory.model.repository.assinatura.AssinaturaRepository;
import org.exemplo.bellory.service.assinatura.AssinaturaCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Endpoints relacionados ao cache de status da assinatura consumido via Payment API.
 *
 * Ponto chave: o cache (Redis, fresh 5min + stale 24h) so e invalidado explicitamente.
 * O frontend deve chamar POST /refresh-cache apos qualquer acao feita diretamente na
 * Payment API (troca de plano, pagamento confirmado, etc.) para que o Bellory enxergue
 * o novo estado imediatamente.
 */
@RestController
@RequestMapping("/api/v1/assinatura")
@CrossOrigin(origins = {"https://bellory.vercel.app", "https://*.vercel.app", "http://localhost:*"})
@Tag(name = "Assinatura", description = "Operacoes de assinatura / cache Payment API")
@Slf4j
public class AssinaturaCacheController {

    private final AssinaturaCacheService assinaturaCacheService;
    private final AssinaturaRepository assinaturaRepository;
    private final PaymentApiClient paymentApiClient;

    public AssinaturaCacheController(AssinaturaCacheService assinaturaCacheService,
                                     AssinaturaRepository assinaturaRepository,
                                     PaymentApiClient paymentApiClient) {
        this.assinaturaCacheService = assinaturaCacheService;
        this.assinaturaRepository = assinaturaRepository;
        this.paymentApiClient = paymentApiClient;
    }

    @Operation(summary = "Retorna o status atual da assinatura (le do cache Redis, fresh 5min + stale 24h; sem invalidar)")
    @GetMapping("/status")
    public ResponseEntity<AssinaturaStatusDTO> getStatus() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AssinaturaStatusDTO status = assinaturaCacheService.getStatusByOrganizacao(organizacaoId);
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "Invalida o cache e refaz o fetch do status na Payment API")
    @PostMapping("/refresh-cache")
    public ResponseEntity<AssinaturaStatusDTO> refreshCache() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AssinaturaStatusDTO status = assinaturaCacheService.refreshByOrganizacao(organizacaoId);
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "Retorna o agregado {assinatura, plano, cobrancas} consultando a Payment API em tempo real pela organizacao logada")
    @GetMapping("/cobrancas")
    public ResponseEntity<AssinaturaCompletaDTO> getCobrancas() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Assinatura> optAssinatura = assinaturaRepository.findByOrganizacaoId(organizacaoId);
        if (optAssinatura.isEmpty() || optAssinatura.get().getPaymentApiSubscriptionId() == null) {
            log.warn("Assinatura ou paymentApiSubscriptionId nao encontrado para organizacao {}", organizacaoId);
            return ResponseEntity.ok(AssinaturaCompletaDTO.builder()
                    .cobrancas(Collections.emptyList())
                    .build());
        }

        Long subscriptionId = optAssinatura.get().getPaymentApiSubscriptionId();
        try {
            SubscriptionResponse assinatura = paymentApiClient.getSubscription(subscriptionId);

            PlanResponse plano = null;
            if (assinatura != null && assinatura.getPlanId() != null) {
                try {
                    plano = paymentApiClient.getPlan(assinatura.getPlanId());
                } catch (PaymentApiException e) {
                    log.warn("Falha consultando plano {} (subscriptionId={}): {}",
                            assinatura.getPlanId(), subscriptionId, e.getMessage());
                }
            }

            List<ChargeResponse> cobrancas;
            try {
                cobrancas = paymentApiClient.listChargesBySubscription(subscriptionId);
            } catch (PaymentApiException e) {
                log.warn("Falha consultando cobrancas (subscriptionId={}): {}", subscriptionId, e.getMessage());
                cobrancas = Collections.emptyList();
            }

            return ResponseEntity.ok(AssinaturaCompletaDTO.builder()
                    .assinatura(assinatura)
                    .plano(plano)
                    .cobrancas(cobrancas)
                    .build());
        } catch (PaymentApiException e) {
            log.error("Falha consultando assinatura na Payment API (subscriptionId={}): {}", subscriptionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
