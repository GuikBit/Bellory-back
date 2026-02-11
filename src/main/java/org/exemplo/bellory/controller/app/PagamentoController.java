package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.compra.CobrancaDTO;
import org.exemplo.bellory.model.dto.compra.PagamentoCreateDTO;
import org.exemplo.bellory.model.dto.compra.PagamentoDTO;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.pagamento.Pagamento;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.exemplo.bellory.service.TransacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pagamentos")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Processamento de pagamentos e gerenciamento de cobranças")
public class PagamentoController {

    private final TransacaoService transacaoService;
    private final CobrancaRepository cobrancaRepository;

    /**
     * Processa pagamento de uma cobrança
     */
    @Operation(summary = "Processar pagamento de cobrança")
    @PostMapping
    public ResponseEntity<ResponseAPI<PagamentoDTO>> processarPagamento(
            @RequestBody PagamentoCreateDTO dto) {
        try {
            // Validações básicas
            if (dto.getCobrancaId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<PagamentoDTO>builder()
                                .success(false)
                                .message("O ID da cobrança é obrigatório.")
                                .errorCode(400)
                                .build());
            }

            if (dto.getValor() == null || dto.getValor().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<PagamentoDTO>builder()
                                .success(false)
                                .message("O valor do pagamento deve ser maior que zero.")
                                .errorCode(400)
                                .build());
            }

            if (dto.getFormaPagamento() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<PagamentoDTO>builder()
                                .success(false)
                                .message("A forma de pagamento é obrigatória.")
                                .errorCode(400)
                                .build());
            }

            // Processar pagamento
            Pagamento pagamento = transacaoService.processarPagamento(
                    dto.getCobrancaId(),
                    dto.getValor(),
                    dto.getFormaPagamento()
            );

            PagamentoDTO pagamentoDTO = new PagamentoDTO(pagamento);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<PagamentoDTO>builder()
                            .success(true)
                            .message("Pagamento processado com sucesso.")
                            .dados(pagamentoDTO)
                            .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<PagamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<PagamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<PagamentoDTO>builder()
                            .success(false)
                            .message("Erro ao processar pagamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Busca detalhes de uma cobrança
     */
    @Operation(summary = "Buscar detalhes de uma cobrança")
    @GetMapping("/cobranca/{cobrancaId}")
    public ResponseEntity<ResponseAPI<CobrancaDTO>> getCobranca(@PathVariable Long cobrancaId) {
        try {
            Cobranca cobranca = cobrancaRepository.findById(cobrancaId)
                    .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada."));

            CobrancaDTO cobrancaDTO = new CobrancaDTO(cobranca);

            return ResponseEntity.ok(ResponseAPI.<CobrancaDTO>builder()
                    .success(true)
                    .message("Cobrança encontrada.")
                    .dados(cobrancaDTO)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<CobrancaDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CobrancaDTO>builder()
                            .success(false)
                            .message("Erro ao buscar cobrança: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Lista cobranças de um agendamento
     */
    @Operation(summary = "Listar cobranças de um agendamento")
    @GetMapping("/agendamento/{agendamentoId}/cobrancas")
    public ResponseEntity<ResponseAPI<List<CobrancaDTO>>> getCobrancasPorAgendamento(
            @PathVariable Long agendamentoId) {
        try {
            List<Cobranca> cobrancas = cobrancaRepository.findByAgendamentoId(agendamentoId);

            List<CobrancaDTO> cobrancasDTO = cobrancas.stream()
                    .map(CobrancaDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ResponseAPI.<List<CobrancaDTO>>builder()
                    .success(true)
                    .message("Cobranças encontradas.")
                    .dados(cobrancasDTO)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CobrancaDTO>>builder()
                            .success(false)
                            .message("Erro ao buscar cobranças: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Cancela uma cobrança
     */
    @Operation(summary = "Cancelar uma cobrança")
    @DeleteMapping("/{cobrancaId}")
    public ResponseEntity<ResponseAPI<Void>> cancelarCobranca(
            @PathVariable Long cobrancaId,
            @RequestParam(required = false) String motivo) {
        try {
            String motivoCancelamento = motivo != null ? motivo : "Cancelado pelo usuário";
            transacaoService.cancelarCobranca(cobrancaId, motivoCancelamento);

            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Cobrança cancelada com sucesso.")
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao cancelar cobrança: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ========================================================================
    // ENDPOINTS PARA INTEGRAÇÃO COM STRIPE (PREPARADO PARA FUTURO)
    // ========================================================================

    /*
    /**
     * Cria PaymentIntent no Stripe para uma cobrança
     */
    /*
    @PostMapping("/stripe/criar-payment-intent")
    public ResponseEntity<ResponseAPI<StripePaymentIntentDTO>> criarPaymentIntent(
            @RequestBody StripePaymentIntentCreateDTO dto) {
        try {
            PaymentIntent paymentIntent = transacaoService.criarPaymentIntentStripe(
                    dto.getCobrancaId()
            );

            StripePaymentIntentDTO resultado = new StripePaymentIntentDTO(
                    paymentIntent.getId(),
                    paymentIntent.getClientSecret(),
                    paymentIntent.getAmount(),
                    paymentIntent.getStatus()
            );

            return ResponseEntity.ok(ResponseAPI.<StripePaymentIntentDTO>builder()
                    .success(true)
                    .message("PaymentIntent criado com sucesso.")
                    .dados(resultado)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<StripePaymentIntentDTO>builder()
                            .success(false)
                            .message("Erro ao criar PaymentIntent: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
    */

    /*
    /**
     * Webhook do Stripe para processar eventos
     */
    /*
    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> webhookStripe(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // Verificar assinatura do webhook
            Event event = Webhook.constructEvent(
                    payload,
                    sigHeader,
                    stripeWebhookSecret
            );

            // Processar evento
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow();
                    transacaoService.processarWebhookStripePagamentoConfirmado(
                            paymentIntent.getId()
                    );
                    break;

                case "payment_intent.payment_failed":
                    PaymentIntent failedIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow();
                    transacaoService.processarWebhookStripePagamentoFalhou(
                            failedIntent.getId(),
                            failedIntent.getLastPaymentError().getMessage()
                    );
                    break;

                default:
                    // Outros eventos ignorados
                    break;
            }

            return ResponseEntity.ok("Webhook processado");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro ao processar webhook: " + e.getMessage());
        }
    }
    */
}
