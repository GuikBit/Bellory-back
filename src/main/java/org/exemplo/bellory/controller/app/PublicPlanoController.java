package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.client.payment.PaymentApiClient;
import org.exemplo.bellory.client.payment.dto.PlanResponse;
import org.exemplo.bellory.exception.PaymentApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/planos")
@CrossOrigin(origins = {"https://bellory.vercel.app", "https://*.vercel.app", "http://localhost:*"})
@Tag(name = "Planos (Público)", description = "Listagem pública de planos para landing page - sem autenticação")
@Slf4j
public class PublicPlanoController {

    private final PaymentApiClient paymentApiClient;

    public PublicPlanoController(PaymentApiClient paymentApiClient) {
        this.paymentApiClient = paymentApiClient;
    }

    @Operation(summary = "Lista todos os planos ativos (público, sem autenticação)")
    @GetMapping
    public ResponseEntity<List<PlanResponse>> listarPlanos() {
        try {
            List<PlanResponse> planos = paymentApiClient.listPlans();
            return ResponseEntity.ok(planos);
        } catch (PaymentApiException e) {
            log.error("Falha ao listar planos (público): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @Operation(summary = "Busca um plano por código (público, sem autenticação)")
    @GetMapping("/{codigo}")
    public ResponseEntity<PlanResponse> buscarPlanoPorCodigo(@PathVariable String codigo) {
        try {
            PlanResponse plano = paymentApiClient.getPlanByCodigo(codigo);
            return ResponseEntity.ok(plano);
        } catch (PaymentApiException e) {
            log.error("Falha ao buscar plano '{}' (público): {}", codigo, e.getMessage());
            return ResponseEntity.status(e.getStatusCode() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY).build();
        }
    }
}
