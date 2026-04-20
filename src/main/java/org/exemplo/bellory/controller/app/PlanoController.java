package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.client.payment.PaymentApiClient;
import org.exemplo.bellory.client.payment.dto.*;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.exception.PaymentApiException;
import org.exemplo.bellory.model.dto.assinatura.TrocarPlanoRequestDTO;
import org.exemplo.bellory.model.entity.assinatura.Assinatura;
import org.exemplo.bellory.model.repository.assinatura.AssinaturaRepository;
import org.exemplo.bellory.service.assinatura.AssinaturaCacheService;
import org.exemplo.bellory.service.plano.PlanoUsoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/planos")
@CrossOrigin(origins = {"https://bellory.vercel.app", "https://*.vercel.app", "http://localhost:*"})
@Tag(name = "Planos", description = "Listagem de planos, troca de plano, cancelamento e cupons")
@Slf4j
public class PlanoController {

    private final PaymentApiClient paymentApiClient;
    private final AssinaturaRepository assinaturaRepository;
    private final AssinaturaCacheService assinaturaCacheService;
    private final PlanoUsoService planoUsoService;

    public PlanoController(PaymentApiClient paymentApiClient,
                           AssinaturaRepository assinaturaRepository,
                           AssinaturaCacheService assinaturaCacheService,
                           PlanoUsoService planoUsoService) {
        this.paymentApiClient = paymentApiClient;
        this.assinaturaRepository = assinaturaRepository;
        this.assinaturaCacheService = assinaturaCacheService;
        this.planoUsoService = planoUsoService;
    }

    // ==================== LISTAR PLANOS ====================

    @Operation(summary = "Lista todos os planos ativos disponiveis para contratacao")
    @GetMapping
    public ResponseEntity<List<PlanResponse>> listarPlanos() {
        try {
            List<PlanResponse> planos = paymentApiClient.listPlans();
            return ResponseEntity.ok(planos);
        } catch (PaymentApiException e) {
            log.error("Falha ao listar planos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @Operation(summary = "Busca um plano por codigo (ex: basico, plus, premium)")
    @GetMapping("/{codigo}")
    public ResponseEntity<PlanResponse> buscarPlanoPorCodigo(@PathVariable String codigo) {
        try {
            PlanResponse plano = paymentApiClient.getPlanByCodigo(codigo);
            return ResponseEntity.ok(plano);
        } catch (PaymentApiException e) {
            log.error("Falha ao buscar plano '{}': {}", codigo, e.getMessage());
            return ResponseEntity.status(e.getStatusCode() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY).build();
        }
    }

    // ==================== PREVIEW TROCA DE PLANO ====================

    @Operation(summary = "Simula a troca de plano sem efetivar (mostra valores, pro-rata, tipo de mudanca)")
    @PostMapping("/preview-troca")
    public ResponseEntity<?> previewTrocaPlano(@RequestParam Long novoPlanId) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Assinatura assinatura = getAssinaturaOrThrow(organizacaoId);

        try {
            PlanChangePreviewResponse preview = paymentApiClient.previewPlanChange(
                    assinatura.getPaymentApiSubscriptionId(), novoPlanId);
            return ResponseEntity.ok(preview);
        } catch (PaymentApiException e) {
            log.error("Falha no preview de troca de plano (org={}, novoPlanId={}): {}",
                    organizacaoId, novoPlanId, e.getMessage());
            return buildPaymentErrorResponse(e);
        }
    }

    // ==================== TROCAR PLANO ====================

    @Operation(summary = "Efetiva a troca de plano da organizacao logada")
    @PostMapping("/trocar")
    public ResponseEntity<?> trocarPlano(@RequestBody TrocarPlanoRequestDTO body, HttpServletRequest request) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (body.getNewPlanId() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "newPlanId é obrigatório"));
        }

        Assinatura assinatura = getAssinaturaOrThrow(organizacaoId);

        // Monta uso atual para validacao de downgrade
        Map<String, Integer> currentUsage = planoUsoService.getUsageMap(organizacaoId);

        String requestedBy = TenantContext.getCurrentUsername();
        String remoteIp = request.getRemoteAddr();

        // Resolve billingType do body (default UNDEFINED se não informado)
        PaymentBillingType billingType = null;
        if (body.getBillingType() != null && !body.getBillingType().isBlank()) {
            billingType = PaymentBillingType.valueOf(body.getBillingType().toUpperCase());
        }

        RequestPlanChangeRequest req = RequestPlanChangeRequest.builder()
                .newPlanId(body.getNewPlanId())
                .currentUsage(currentUsage)
                .requestedBy(requestedBy)
                .remoteIp(remoteIp)
                .billingType(billingType)
                .creditCard(body.getCreditCard())
                .creditCardHolderInfo(body.getCreditCardHolderInfo())
                .creditCardToken(body.getCreditCardToken())
                .build();

        try {
            PlanChangeResponse planChange = paymentApiClient.changePlan(
                    assinatura.getPaymentApiSubscriptionId(), req);

            // Invalida cache para refletir o novo plano imediatamente
            assinaturaCacheService.refreshByOrganizacao(organizacaoId);

            // Se gerou cobranca (upgrade com pro-rata), busca os dados de pagamento
            ChargeResponse cobranca = null;
            if (planChange.getChargeId() != null) {
                try {
                    cobranca = paymentApiClient.getCharge(planChange.getChargeId());
                } catch (PaymentApiException ce) {
                    log.warn("Troca de plano OK, mas falha ao buscar cobranca {}: {}",
                            planChange.getChargeId(), ce.getMessage());
                }
            }

            return ResponseEntity.ok(TrocaPlanoResponseDTO.builder()
                    .trocaPlano(planChange)
                    .cobranca(cobranca)
                    .build());
        } catch (PaymentApiException e) {
            log.error("Falha na troca de plano (org={}, novoPlanId={}): {}",
                    organizacaoId, body.getNewPlanId(), e.getMessage());
            return buildPaymentErrorResponse(e);
        }
    }

    // ==================== CANCELAR ASSINATURA ====================

    @Operation(summary = "Cancela a assinatura da organizacao logada")
    @PostMapping("/cancelar")
    public ResponseEntity<?> cancelarAssinatura() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Assinatura assinatura = getAssinaturaOrThrow(organizacaoId);

        try {
            SubscriptionResponse response = paymentApiClient.cancelSubscription(
                    assinatura.getPaymentApiSubscriptionId());

            // Invalida cache para refletir o cancelamento
            assinaturaCacheService.refreshByOrganizacao(organizacaoId);

            return ResponseEntity.ok(response);
        } catch (PaymentApiException e) {
            log.error("Falha ao cancelar assinatura (org={}): {}", organizacaoId, e.getMessage());
            return buildPaymentErrorResponse(e);
        }
    }

    // ==================== VALIDAR CUPOM ====================

    @Operation(summary = "Valida um cupom de desconto (endpoint publico na Payment API, sem vincular ao customer)")
    @PostMapping("/cupom/validar")
    public ResponseEntity<?> validarCupom(@RequestBody ValidateCouponRequest req) {
        try {
            CouponValidationResponse response = paymentApiClient.validateCouponPublic(req);
            return ResponseEntity.ok(response);
        } catch (PaymentApiException e) {
            log.error("Falha ao validar cupom '{}': {}", req.getCouponCode(), e.getMessage());
            return buildPaymentErrorResponse(e);
        }
    }

    // ==================== HELPERS ====================

    private Assinatura getAssinaturaOrThrow(Long organizacaoId) {
        Optional<Assinatura> opt = assinaturaRepository.findByOrganizacaoId(organizacaoId);
        if (opt.isEmpty() || opt.get().getPaymentApiSubscriptionId() == null) {
            throw new IllegalStateException("Assinatura nao encontrada para a organizacao");
        }
        return opt.get();
    }

    private ResponseEntity<?> buildPaymentErrorResponse(PaymentApiException e) {
        int code = e.getStatusCode();
        if (code == 0) code = 502;

        HttpStatus status = HttpStatus.valueOf(Math.min(code, 599));
        return ResponseEntity.status(status)
                .body(Map.of(
                        "success", false,
                        "message", e.getMessage(),
                        "details", e.getResponseBody() != null ? e.getResponseBody() : ""
                ));
    }
}
