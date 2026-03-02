package org.exemplo.bellory.service.assinatura;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.assinatura.*;
import org.exemplo.bellory.model.dto.assinatura.assas.*;
import org.exemplo.bellory.model.entity.assinatura.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.exemplo.bellory.model.repository.assinatura.AssinaturaRepository;
import org.exemplo.bellory.model.repository.assinatura.CobrancaPlataformaRepository;
import org.exemplo.bellory.model.repository.assinatura.PagamentoPlataformaRepository;
import org.exemplo.bellory.model.repository.organizacao.PlanoBelloryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final CobrancaPlataformaRepository cobrancaPlataformaRepository;
    private final PagamentoPlataformaRepository pagamentoPlataformaRepository;
    private final PlanoBelloryRepository planoBelloryRepository;
    private final AssasClient assasClient;

    @Value("${bellory.trial.dias:14}")
    private int diasTrial;

    public AssinaturaService(AssinaturaRepository assinaturaRepository,
                             CobrancaPlataformaRepository cobrancaPlataformaRepository,
                             PagamentoPlataformaRepository pagamentoPlataformaRepository,
                             PlanoBelloryRepository planoBelloryRepository,
                             AssasClient assasClient) {
        this.assinaturaRepository = assinaturaRepository;
        this.cobrancaPlataformaRepository = cobrancaPlataformaRepository;
        this.pagamentoPlataformaRepository = pagamentoPlataformaRepository;
        this.planoBelloryRepository = planoBelloryRepository;
        this.assasClient = assasClient;
    }

    // ==================== TRIAL ====================

    @Transactional
    public Assinatura criarAssinaturaTrial(Organizacao organizacao, PlanoBellory plano) {
        log.info("Criando assinatura trial para organizacao: {}", organizacao.getId());

        LocalDateTime agora = LocalDateTime.now();
        Assinatura assinatura = Assinatura.builder()
                .organizacao(organizacao)
                .planoBellory(plano)
                .status(StatusAssinatura.TRIAL)
                .cicloCobranca(CicloCobranca.MENSAL)
                .dtInicioTrial(agora)
                .dtFimTrial(agora.plusDays(diasTrial))
                .valorMensal(plano.getPrecoMensal())
                .valorAnual(plano.getPrecoAnual())
                .build();

        return assinaturaRepository.save(assinatura);
    }

    // ==================== STATUS ====================

    @Transactional(readOnly = true)
    public AssinaturaStatusDTO getStatusAssinatura(Long organizacaoId) {
        return assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .map(this::buildStatusDTO)
                .orElse(AssinaturaStatusDTO.builder()
                        .bloqueado(false)
                        .statusAssinatura("SEM_ASSINATURA")
                        .mensagem("Nenhuma assinatura encontrada")
                        .build());
    }

    private AssinaturaStatusDTO buildStatusDTO(Assinatura assinatura) {
        AssinaturaStatusDTO.AssinaturaStatusDTOBuilder builder = AssinaturaStatusDTO.builder()
                .statusAssinatura(assinatura.getStatus().name())
                .planoCodigo(assinatura.getPlanoBellory() != null ? assinatura.getPlanoBellory().getCodigo() : null)
                .planoNome(assinatura.getPlanoBellory() != null ? assinatura.getPlanoBellory().getNome() : null);

        switch (assinatura.getStatus()) {
            case TRIAL -> {
                if (assinatura.isTrialExpirado()) {
                    builder.bloqueado(true);
                    builder.mensagem("Seu periodo de teste expirou. Escolha um plano para continuar.");
                    builder.diasRestantesTrial(0);
                } else {
                    long diasRestantes = ChronoUnit.DAYS.between(LocalDateTime.now(), assinatura.getDtFimTrial());
                    builder.bloqueado(false);
                    builder.diasRestantesTrial((int) Math.max(diasRestantes, 0));
                    builder.mensagem("Voce esta no periodo de teste. Restam " + diasRestantes + " dias.");
                }
            }
            case ATIVA -> {
                builder.bloqueado(false);
                builder.mensagem("Assinatura ativa.");
            }
            case VENCIDA -> {
                builder.bloqueado(true);
                builder.mensagem("Sua assinatura esta vencida. Regularize o pagamento para continuar.");
            }
            case CANCELADA -> {
                builder.bloqueado(true);
                builder.mensagem("Sua assinatura foi cancelada. Escolha um plano para reativar.");
            }
            case SUSPENSA -> {
                builder.bloqueado(true);
                builder.mensagem("Sua assinatura esta suspensa. Entre em contato com o suporte.");
            }
        }

        return builder.build();
    }

    // ==================== ESCOLHER PLANO ====================

    @Transactional
    public AssinaturaResponseDTO escolherPlano(EscolherPlanoDTO dto) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organizacao nao identificada no token");
        }

        Assinatura assinatura = assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalStateException("Assinatura nao encontrada"));

        PlanoBellory plano = planoBelloryRepository.findByCodigo(dto.getPlanoCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Plano nao encontrado: " + dto.getPlanoCodigo()));

        if (!plano.isAtivo()) {
            throw new IllegalArgumentException("Plano indisponivel");
        }

        CicloCobranca ciclo = CicloCobranca.valueOf(dto.getCicloCobranca());
        FormaPagamentoPlataforma forma = FormaPagamentoPlataforma.valueOf(dto.getFormaPagamento());

        BigDecimal valor = ciclo == CicloCobranca.ANUAL ? plano.getPrecoAnual() : plano.getPrecoMensal();

        // Tentar criar no Assas
        if (assinatura.getAssasCustomerId() == null) {
            Organizacao org = assinatura.getOrganizacao();
            AssasCustomerResponse customer = assasClient.criarCliente(
                    AssasCustomerRequest.builder()
                            .name(org.getNomeFantasia())
                            .cpfCnpj(org.getCnpj())
                            .email(org.getEmailPrincipal())
                            .phone(org.getTelefone1())
                            .build()
            );
            if (customer != null) {
                assinatura.setAssasCustomerId(customer.getId());
            }
        }

        if (assinatura.getAssasCustomerId() != null) {
            String billingType = mapFormaPagamento(forma);
            String cycle = ciclo == CicloCobranca.ANUAL ? "YEARLY" : "MONTHLY";

            AssasSubscriptionResponse sub = assasClient.criarAssinatura(
                    AssasSubscriptionRequest.builder()
                            .customer(assinatura.getAssasCustomerId())
                            .billingType(billingType)
                            .value(valor)
                            .cycle(cycle)
                            .nextDueDate(LocalDate.now().plusDays(1).toString())
                            .description("Assinatura Bellory - Plano " + plano.getNome())
                            .build()
            );
            if (sub != null) {
                assinatura.setAssasSubscriptionId(sub.getId());
            }
        }

        // Atualizar assinatura
        assinatura.setPlanoBellory(plano);
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setCicloCobranca(ciclo);
        assinatura.setDtInicio(LocalDateTime.now());
        assinatura.setValorMensal(plano.getPrecoMensal());
        assinatura.setValorAnual(plano.getPrecoAnual());

        if (ciclo == CicloCobranca.ANUAL) {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusYears(1));
        } else {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusMonths(1));
        }

        assinaturaRepository.save(assinatura);

        // Criar primeira cobranca
        CobrancaPlataforma cobranca = CobrancaPlataforma.builder()
                .assinatura(assinatura)
                .organizacao(assinatura.getOrganizacao())
                .valor(valor)
                .dtVencimento(LocalDate.now().plusDays(1))
                .status(StatusCobrancaPlataforma.PENDENTE)
                .formaPagamento(forma)
                .referenciaMes(LocalDate.now().getMonthValue())
                .referenciaAno(LocalDate.now().getYear())
                .build();
        cobrancaPlataformaRepository.save(cobranca);

        return toAssinaturaResponseDTO(assinatura);
    }

    private String mapFormaPagamento(FormaPagamentoPlataforma forma) {
        return switch (forma) {
            case PIX -> "PIX";
            case BOLETO -> "BOLETO";
            case CARTAO_CREDITO -> "CREDIT_CARD";
        };
    }

    // ==================== COBRANCAS ====================

    @Transactional(readOnly = true)
    public List<CobrancaPlataformaDTO> getMinhasCobrancas(String statusFiltro) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organizacao nao identificada no token");
        }

        List<CobrancaPlataforma> cobrancas;
        if (statusFiltro != null && !statusFiltro.isBlank()) {
            StatusCobrancaPlataforma status = StatusCobrancaPlataforma.valueOf(statusFiltro);
            cobrancas = cobrancaPlataformaRepository.findByOrganizacaoIdAndStatus(organizacaoId, status);
        } else {
            cobrancas = cobrancaPlataformaRepository.findByOrganizacaoId(organizacaoId);
        }

        return cobrancas.stream()
                .map(this::toCobrancaDTO)
                .collect(Collectors.toList());
    }

    // ==================== WEBHOOK ====================

    @Transactional
    public void processarWebhookPagamento(AssasWebhookPayload payload) {
        if (payload == null || payload.getPayment() == null) {
            log.warn("Webhook Assas recebido com payload invalido");
            return;
        }

        String event = payload.getEvent();
        AssasWebhookPayload.Payment payment = payload.getPayment();
        log.info("Webhook Assas recebido - evento: {}, paymentId: {}", event, payment.getId());

        if ("PAYMENT_RECEIVED".equals(event) || "PAYMENT_CONFIRMED".equals(event)) {
            confirmarPagamento(payment);
        } else if ("PAYMENT_OVERDUE".equals(event)) {
            marcarCobrancaVencida(payment);
        }
    }

    private void confirmarPagamento(AssasWebhookPayload.Payment payment) {
        cobrancaPlataformaRepository.findByAssasPaymentId(payment.getId())
                .ifPresent(cobranca -> {
                    cobranca.setStatus(StatusCobrancaPlataforma.PAGA);
                    cobranca.setDtPagamento(LocalDateTime.now());
                    cobrancaPlataformaRepository.save(cobranca);

                    // Registrar pagamento
                    PagamentoPlataforma pagamento = PagamentoPlataforma.builder()
                            .cobranca(cobranca)
                            .valor(payment.getValue())
                            .status(StatusPagamentoPlataforma.CONFIRMADO)
                            .formaPagamento(cobranca.getFormaPagamento())
                            .assasPaymentId(payment.getId())
                            .dtPagamento(LocalDateTime.now())
                            .build();
                    pagamentoPlataformaRepository.save(pagamento);

                    // Garantir que assinatura esta ativa
                    Assinatura assinatura = cobranca.getAssinatura();
                    if (assinatura.getStatus() != StatusAssinatura.ATIVA) {
                        assinatura.setStatus(StatusAssinatura.ATIVA);
                        assinaturaRepository.save(assinatura);
                    }

                    log.info("Pagamento confirmado para cobranca ID: {}", cobranca.getId());
                });
    }

    private void marcarCobrancaVencida(AssasWebhookPayload.Payment payment) {
        cobrancaPlataformaRepository.findByAssasPaymentId(payment.getId())
                .ifPresent(cobranca -> {
                    cobranca.setStatus(StatusCobrancaPlataforma.VENCIDA);
                    cobrancaPlataformaRepository.save(cobranca);
                    log.info("Cobranca marcada como vencida - ID: {}", cobranca.getId());
                });
    }

    // ==================== SCHEDULER ====================

    @Transactional
    public void expirarTrials() {
        List<Assinatura> trialsExpirados = assinaturaRepository.findTrialsExpirados(LocalDateTime.now());
        for (Assinatura assinatura : trialsExpirados) {
            assinatura.setStatus(StatusAssinatura.VENCIDA);
            assinaturaRepository.save(assinatura);
            log.info("Trial expirado para organizacao ID: {}", assinatura.getOrganizacao().getId());
        }
        if (!trialsExpirados.isEmpty()) {
            log.info("Total de trials expirados: {}", trialsExpirados.size());
        }
    }

    @Transactional
    public void marcarCobrancasVencidas() {
        List<CobrancaPlataforma> cobrancasVencidas = cobrancaPlataformaRepository.findCobrancasVencidas(LocalDate.now());
        for (CobrancaPlataforma cobranca : cobrancasVencidas) {
            cobranca.setStatus(StatusCobrancaPlataforma.VENCIDA);
            cobrancaPlataformaRepository.save(cobranca);

            // Se todas as cobrancas pendentes estao vencidas, marcar assinatura como vencida
            Assinatura assinatura = cobranca.getAssinatura();
            if (assinatura.getStatus() == StatusAssinatura.ATIVA) {
                assinatura.setStatus(StatusAssinatura.VENCIDA);
                assinaturaRepository.save(assinatura);
            }
        }
        if (!cobrancasVencidas.isEmpty()) {
            log.info("Total de cobrancas marcadas como vencidas: {}", cobrancasVencidas.size());
        }
    }

    @Transactional
    public void gerarCobrancasMensais() {
        List<Assinatura> ativas = assinaturaRepository.findAtivasComVencimento();
        int mesAtual = LocalDate.now().getMonthValue();
        int anoAtual = LocalDate.now().getYear();

        for (Assinatura assinatura : ativas) {
            boolean jaExiste = cobrancaPlataformaRepository
                    .existsByAssinaturaIdAndReferenciaMesAndReferenciaAno(assinatura.getId(), mesAtual, anoAtual);

            if (!jaExiste) {
                BigDecimal valor = assinatura.getCicloCobranca() == CicloCobranca.ANUAL
                        ? assinatura.getValorAnual()
                        : assinatura.getValorMensal();

                if (valor != null && valor.compareTo(BigDecimal.ZERO) > 0) {
                    CobrancaPlataforma cobranca = CobrancaPlataforma.builder()
                            .assinatura(assinatura)
                            .organizacao(assinatura.getOrganizacao())
                            .valor(valor)
                            .dtVencimento(LocalDate.now().withDayOfMonth(10))
                            .status(StatusCobrancaPlataforma.PENDENTE)
                            .referenciaMes(mesAtual)
                            .referenciaAno(anoAtual)
                            .build();
                    cobrancaPlataformaRepository.save(cobranca);
                    log.info("Cobranca gerada para assinatura ID: {}", assinatura.getId());
                }
            }
        }
    }

    // ==================== CONVERSORES ====================

    public AssinaturaResponseDTO toAssinaturaResponseDTO(Assinatura assinatura) {
        return AssinaturaResponseDTO.builder()
                .id(assinatura.getId())
                .organizacaoId(assinatura.getOrganizacao().getId())
                .organizacaoNome(assinatura.getOrganizacao().getNomeFantasia())
                .planoBelloryId(assinatura.getPlanoBellory().getId())
                .planoNome(assinatura.getPlanoBellory().getNome())
                .planoCodigo(assinatura.getPlanoBellory().getCodigo())
                .status(assinatura.getStatus().name())
                .cicloCobranca(assinatura.getCicloCobranca().name())
                .dtInicioTrial(assinatura.getDtInicioTrial())
                .dtFimTrial(assinatura.getDtFimTrial())
                .dtInicio(assinatura.getDtInicio())
                .dtProximoVencimento(assinatura.getDtProximoVencimento())
                .dtCancelamento(assinatura.getDtCancelamento())
                .valorMensal(assinatura.getValorMensal())
                .valorAnual(assinatura.getValorAnual())
                .assasCustomerId(assinatura.getAssasCustomerId())
                .assasSubscriptionId(assinatura.getAssasSubscriptionId())
                .dtCriacao(assinatura.getDtCriacao())
                .build();
    }

    private CobrancaPlataformaDTO toCobrancaDTO(CobrancaPlataforma cobranca) {
        return CobrancaPlataformaDTO.builder()
                .id(cobranca.getId())
                .valor(cobranca.getValor())
                .dtVencimento(cobranca.getDtVencimento())
                .dtPagamento(cobranca.getDtPagamento())
                .status(cobranca.getStatus().name())
                .formaPagamento(cobranca.getFormaPagamento() != null ? cobranca.getFormaPagamento().name() : null)
                .assasInvoiceUrl(cobranca.getAssasInvoiceUrl())
                .assasBankSlipUrl(cobranca.getAssasBankSlipUrl())
                .assasPixQrCode(cobranca.getAssasPixQrCode())
                .assasPixCopiaCola(cobranca.getAssasPixCopiaCola())
                .referenciaMes(cobranca.getReferenciaMes())
                .referenciaAno(cobranca.getReferenciaAno())
                .dtCriacao(cobranca.getDtCriacao())
                .build();
    }
}
