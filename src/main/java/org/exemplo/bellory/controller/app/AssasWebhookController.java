package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.assinatura.assas.AssasWebhookPayload;
import org.exemplo.bellory.service.assinatura.AssinaturaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhook/assas")
@Slf4j
@Tag(name = "Webhook Assas", description = "Receber notificacoes de pagamento do Assas")
public class AssasWebhookController {

    private final AssinaturaService assinaturaService;

    @Value("${assas.webhook.token:}")
    private String webhookToken;

    public AssasWebhookController(AssinaturaService assinaturaService) {
        this.assinaturaService = assinaturaService;
    }

    @PostMapping
    @Operation(summary = "Receber webhook de pagamento do Assas")
    public ResponseEntity<Void> receberWebhook(
            @RequestBody AssasWebhookPayload payload,
            @RequestHeader(value = "asaas-access-token", required = false) String token) {

        // Validar token do webhook se configurado
        if (webhookToken != null && !webhookToken.isBlank()) {
            if (token == null || !webhookToken.equals(token)) {
                log.warn("Webhook Assas recebido com token invalido");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        try {
            assinaturaService.processarWebhookPagamento(payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Erro ao processar webhook Assas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
