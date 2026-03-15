package org.exemplo.bellory.service.assinatura;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.exception.AssasApiException;
import org.exemplo.bellory.model.dto.assinatura.*;
import org.exemplo.bellory.model.dto.assinatura.assas.*;
import org.exemplo.bellory.model.dto.cupom.CupomValidacaoResponseDTO;
import org.exemplo.bellory.model.dto.cupom.CupomValidacaoResult;
import org.exemplo.bellory.model.dto.cupom.ValidarCupomDTO;
import org.exemplo.bellory.model.entity.assinatura.*;
import org.exemplo.bellory.model.entity.email.EmailTemplate;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.exemplo.bellory.model.repository.assinatura.AssinaturaRepository;
import org.exemplo.bellory.model.repository.assinatura.CobrancaPlataformaRepository;
import org.exemplo.bellory.model.repository.assinatura.CupomUtilizacaoRepository;
import org.exemplo.bellory.model.repository.assinatura.PagamentoPlataformaRepository;
import org.exemplo.bellory.model.repository.assinatura.WebhookLogRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.PlanoBelloryRepository;
import org.exemplo.bellory.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final CobrancaPlataformaRepository cobrancaPlataformaRepository;
    private final PagamentoPlataformaRepository pagamentoPlataformaRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final PlanoBelloryRepository planoBelloryRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final CupomDescontoService cupomDescontoService;
    private final CupomUtilizacaoRepository cupomUtilizacaoRepository;
    private final AssasClient assasClient;
    private final EmailService emailService;

    private static final String PLANO_GRATUITO_CODIGO = "gratuito";

    @Value("${bellory.trial.dias:14}")
    private int diasTrial;

    @Value("${app.url:https://app.bellory.com.br}")
    private String appUrl;

    public AssinaturaService(AssinaturaRepository assinaturaRepository,
                             CobrancaPlataformaRepository cobrancaPlataformaRepository,
                             PagamentoPlataformaRepository pagamentoPlataformaRepository,
                             WebhookLogRepository webhookLogRepository,
                             PlanoBelloryRepository planoBelloryRepository,
                             OrganizacaoRepository organizacaoRepository,
                             CupomDescontoService cupomDescontoService,
                             CupomUtilizacaoRepository cupomUtilizacaoRepository,
                             AssasClient assasClient,
                             EmailService emailService) {
        this.assinaturaRepository = assinaturaRepository;
        this.cobrancaPlataformaRepository = cobrancaPlataformaRepository;
        this.pagamentoPlataformaRepository = pagamentoPlataformaRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.planoBelloryRepository = planoBelloryRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.cupomDescontoService = cupomDescontoService;
        this.cupomUtilizacaoRepository = cupomUtilizacaoRepository;
        this.assasClient = assasClient;
        this.emailService = emailService;
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
                .build();

        // Customer creation moved to escolherPlano() - no Asaas interaction during trial

        return assinaturaRepository.save(assinatura);
    }

    // ==================== STATUS ====================

    @Transactional(readOnly = true)
    public AssinaturaStatusDTO getStatusAssinatura(Long organizacaoId) {
        return assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .map(assinatura -> buildStatusDTO(assinatura, organizacaoId))
                .orElse(AssinaturaStatusDTO.builder()
                        .bloqueado(false)
                        .statusAssinatura("SEM_ASSINATURA")
                        .situacao(SituacaoAssinatura.SEM_ASSINATURA.name())
                        .mensagem("Nenhuma assinatura encontrada")
                        .build());
    }

    @Transactional(readOnly = true)
    public AcessoDTO verificarAcessoPermitido(Long organizacaoId) {
        return assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .map(assinatura -> {
                    boolean bloqueado = assinatura.isBloqueada();
                    String mensagem = null;
                    if (bloqueado) {
                        if (assinatura.isTrialExpirado()) {
                            mensagem = "Seu periodo de teste expirou. Escolha um plano para continuar.";
                        } else {
                            mensagem = switch (assinatura.getStatus()) {
                                case VENCIDA -> "Sua assinatura esta vencida. Regularize o pagamento para continuar.";
                                case CANCELADA -> "Sua assinatura foi cancelada. Escolha um plano para reativar.";
                                case SUSPENSA -> "Sua assinatura esta suspensa. Entre em contato com o suporte.";
                                case AGUARDANDO_PAGAMENTO -> "Aguardando confirmacao do pagamento.";
                                default -> "Acesso bloqueado.";
                            };
                        }
                    }
                    return AcessoDTO.builder()
                            .bloqueado(bloqueado)
                            .statusAssinatura(assinatura.getStatus().name())
                            .mensagem(mensagem)
                            .build();
                })
                .orElse(AcessoDTO.builder()
                        .bloqueado(false)
                        .statusAssinatura("SEM_ASSINATURA")
                        .build());
    }

    private AssinaturaStatusDTO buildStatusDTO(Assinatura assinatura, Long organizacaoId) {
        AssinaturaStatusDTO.AssinaturaStatusDTOBuilder builder = AssinaturaStatusDTO.builder()
                .statusAssinatura(assinatura.getStatus().name())
                .planoCodigo(assinatura.getPlanoBellory() != null ? assinatura.getPlanoBellory().getCodigo() : null)
                .planoNome(assinatura.getPlanoBellory() != null ? assinatura.getPlanoBellory().getNome() : null)
                .planoGratuito(assinatura.isPlanoGratuito())
                .cicloCobranca(assinatura.getCicloCobranca() != null ? assinatura.getCicloCobranca().name() : null)
                .dtProximoVencimento(assinatura.getDtProximoVencimento() != null ? assinatura.getDtProximoVencimento().toLocalDate() : null);

        // Determinar situacao semantica
        SituacaoAssinatura situacao = determinarSituacao(assinatura, organizacaoId);
        builder.situacao(situacao.name());

        switch (situacao) {
            case TRIAL_ATIVO -> {
                long diasRestantes = ChronoUnit.DAYS.between(LocalDateTime.now(), assinatura.getDtFimTrial());
                builder.bloqueado(false);
                builder.diasRestantesTrial((int) Math.max(diasRestantes, 0));
                builder.dtFimTrial(assinatura.getDtFimTrial().toLocalDate());
                builder.mensagem("Voce esta no periodo de teste. Restam " + Math.max(diasRestantes, 0) + " dias.");
            }
            case TRIAL_EXPIRADO -> {
                builder.bloqueado(true);
                builder.diasRestantesTrial(0);
                builder.dtFimTrial(assinatura.getDtFimTrial() != null ? assinatura.getDtFimTrial().toLocalDate() : null);
                builder.mensagem("Seu periodo de teste expirou. Escolha um plano para continuar.");
            }
            case PLANO_GRATUITO -> {
                builder.bloqueado(false);
                builder.mensagem("Voce esta no plano gratuito. Faca upgrade para desbloquear mais recursos.");
            }
            case ATIVA -> {
                builder.bloqueado(false);
                builder.mensagem("Assinatura ativa.");
            }
            case PAGAMENTO_PENDENTE -> {
                builder.bloqueado(false);
                builder.mensagem("Voce tem um pagamento pendente. Regularize para evitar interrupcao do servico.");
            }
            case AGUARDANDO_PAGAMENTO -> {
                builder.bloqueado(false);
                builder.mensagem("Aguardando confirmacao do pagamento. Sua assinatura sera ativada apos o pagamento.");
            }
            case UPGRADE_PENDENTE -> {
                builder.bloqueado(false);
                builder.mensagem("Upgrade pendente de pagamento. Apos confirmacao, seu plano sera atualizado.");
            }
            case DOWNGRADE_AGENDADO -> {
                builder.bloqueado(false);
                builder.mensagem("Downgrade agendado para o proximo ciclo de cobranca.");
            }
            case PAGAMENTO_ATRASADO -> {
                builder.bloqueado(true);
                builder.mensagem("Sua assinatura esta vencida. Regularize o pagamento para continuar.");
            }
            case CANCELADA_COM_ACESSO -> {
                LocalDate dtAcesso = assinatura.getDtProximoVencimento().toLocalDate();
                builder.bloqueado(false);
                builder.dtAcessoAte(dtAcesso);
                builder.mensagem("Sua assinatura foi cancelada. Voce pode usar ate " +
                        dtAcesso.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                        ". Apos isso, sera necessario assinar novamente.");
            }
            case CANCELADA_SEM_ACESSO -> {
                builder.bloqueado(true);
                builder.mensagem("Sua assinatura foi cancelada. Escolha um plano para reativar.");
            }
            case SUSPENSA -> {
                builder.bloqueado(true);
                builder.mensagem("Sua assinatura esta suspensa. Entre em contato com o suporte.");
            }
            case SEM_ASSINATURA -> {
                builder.bloqueado(true);
                builder.mensagem("Nenhuma assinatura encontrada. Escolha um plano.");
            }
        }

        // Informacoes de cobrancas pendentes
        BigDecimal valorPendente = cobrancaPlataformaRepository.somarValorPendente(organizacaoId);
        boolean temPendente = valorPendente != null && valorPendente.compareTo(BigDecimal.ZERO) > 0;
        builder.temCobrancaPendente(temPendente);
        if (temPendente) {
            builder.valorPendente(valorPendente);
            builder.dtVencimentoProximaCobranca(
                    cobrancaPlataformaRepository.findProximoVencimentoPendente(organizacaoId));
        }

        return builder.build();
    }

    private SituacaoAssinatura determinarSituacao(Assinatura assinatura, Long organizacaoId) {
        return switch (assinatura.getStatus()) {
            case TRIAL -> {
                if (assinatura.isTrialExpirado()) {
                    yield SituacaoAssinatura.TRIAL_EXPIRADO;
                }
                yield SituacaoAssinatura.TRIAL_ATIVO;
            }
            case ATIVA -> {
                if (assinatura.isPlanoGratuito()) {
                    yield SituacaoAssinatura.PLANO_GRATUITO;
                }
                // Verificar cobrancas vencidas localmente
                BigDecimal valorPendente = cobrancaPlataformaRepository.somarValorPendente(organizacaoId);
                if (valorPendente != null && valorPendente.compareTo(BigDecimal.ZERO) > 0) {
                    yield SituacaoAssinatura.PAGAMENTO_PENDENTE;
                }
                yield SituacaoAssinatura.ATIVA;
            }
            case AGUARDANDO_PAGAMENTO -> SituacaoAssinatura.AGUARDANDO_PAGAMENTO;
            case UPGRADE_PENDENTE -> SituacaoAssinatura.UPGRADE_PENDENTE;
            case DOWNGRADE_AGENDADO -> SituacaoAssinatura.DOWNGRADE_AGENDADO;
            case VENCIDA -> SituacaoAssinatura.PAGAMENTO_ATRASADO;
            case CANCELADA -> {
                if (assinatura.getDtProximoVencimento() != null
                        && LocalDateTime.now().isBefore(assinatura.getDtProximoVencimento())) {
                    yield SituacaoAssinatura.CANCELADA_COM_ACESSO;
                }
                yield SituacaoAssinatura.CANCELADA_SEM_ACESSO;
            }
            case SUSPENSA -> SituacaoAssinatura.SUSPENSA;
        };
    }

    // ==================== FORMA DE PAGAMENTO ====================

    @Transactional(readOnly = true)
    public FormaPagamentoResponseDTO getFormaPagamento() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organizacao nao identificada no token");
        }

        Assinatura assinatura = assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .orElse(null);

        if (assinatura == null || assinatura.getFormaPagamento() == null) {
            return FormaPagamentoResponseDTO.builder()
                    .possuiFormaPagamento(false)
                    .build();
        }

        FormaPagamentoResponseDTO.FormaPagamentoResponseDTOBuilder builder = FormaPagamentoResponseDTO.builder()
                .formaPagamento(assinatura.getFormaPagamento().name())
                .possuiFormaPagamento(true);

        // Se for cartao de credito, buscar dados seguros do Asaas
        if (assinatura.getFormaPagamento() == FormaPagamentoPlataforma.CARTAO_CREDITO
                && assinatura.getAssasSubscriptionId() != null) {
            try {
                AssasSubscriptionResponse sub = assasClient.buscarAssinatura(assinatura.getAssasSubscriptionId());
                if (sub != null && sub.getCreditCardNumber() != null) {
                    builder.ultimosQuatroDigitos(sub.getCreditCardNumber());
                    builder.bandeira(sub.getCreditCardBrand());
                    builder.creditCardToken(sub.getCreditCardToken());
                }
            } catch (Exception e) {
                log.warn("Nao foi possivel buscar dados do cartao no Asaas para org {}: {}",
                        organizacaoId, e.getMessage());
                // Retorna sem dados do cartao - nao bloqueia o fluxo
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

        BigDecimal valorOriginal = ciclo == CicloCobranca.ANUAL ? plano.getPrecoAnual() : plano.getPrecoMensal();
        BigDecimal valor = valorOriginal;

        // Validar e aplicar cupom de desconto
        CupomValidacaoResult cupomResult = null;
        if (dto.getCodigoCupom() != null && !dto.getCodigoCupom().isBlank()) {
            cupomResult = cupomDescontoService.validarCupom(
                    dto.getCodigoCupom(), assinatura.getOrganizacao(), dto.getPlanoCodigo(), dto.getCicloCobranca(), valorOriginal);
            if (!cupomResult.isValido()) {
                throw new IllegalArgumentException("Cupom invalido: " + cupomResult.getMensagem());
            }
            valor = cupomResult.getValorComDesconto();
        }

        // Determinar valor para o Asaas
        BigDecimal valorAssas = valor;

        // Criar cliente no Asaas se nao existir
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

        // Criar assinatura no Asaas
        if (assinatura.getAssasCustomerId() != null) {
            // Se ja existe assinatura no Asaas, cancela a antiga
            if (assinatura.getAssasSubscriptionId() != null) {
                try {
                    assasClient.cancelarAssinatura(assinatura.getAssasSubscriptionId());
                } catch (AssasApiException e) {
                    log.warn("Erro ao cancelar assinatura antiga no Asaas: {}", e.getMessage());
                }
            }

            String billingType = mapFormaPagamento(forma);
            String cycle = ciclo == CicloCobranca.ANUAL ? "YEARLY" : "MONTHLY";

            AssasSubscriptionResponse sub = assasClient.criarAssinatura(
                    AssasSubscriptionRequest.builder()
                            .customer(assinatura.getAssasCustomerId())
                            .billingType(billingType)
                            .value(valorAssas)
                            .cycle(cycle)
                            .nextDueDate(LocalDate.now().plusDays(1).toString())
                            .description("Assinatura Bellory - Plano " + plano.getNome())
                            .build()
            );
            if (sub != null) {
                assinatura.setAssasSubscriptionId(sub.getId());
            }
        }

        // Atualizar assinatura local
        assinatura.setPlanoBellory(plano);
        assinatura.setCicloCobranca(ciclo);
        assinatura.setFormaPagamento(forma);
        assinatura.setDtInicio(LocalDateTime.now());
        assinatura.setDtCancelamento(null);

        // Set status based on payment method
        if (forma == FormaPagamentoPlataforma.CARTAO_CREDITO) {
            assinatura.setStatus(StatusAssinatura.ATIVA);
        } else {
            // Boleto/PIX: wait for webhook confirmation
            assinatura.setStatus(StatusAssinatura.AGUARDANDO_PAGAMENTO);
        }

        // Setar campos de cupom na assinatura
        if (cupomResult != null && cupomResult.isValido()) {
            assinatura.setCupom(cupomResult.getCupom());
            assinatura.setValorDesconto(cupomResult.getValorDesconto());
            assinatura.setCupomCodigo(cupomResult.getCupom().getCodigo());
        } else {
            assinatura.setCupom(null);
            assinatura.setValorDesconto(null);
            assinatura.setCupomCodigo(null);
        }

        if (ciclo == CicloCobranca.ANUAL) {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusYears(1));
        } else {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusMonths(1));
        }

        assinaturaRepository.save(assinatura);

        // Registrar utilizacao do cupom
        if (cupomResult != null && cupomResult.isValido()) {
            cupomDescontoService.registrarUtilizacao(
                    cupomResult.getCupom(),
                    organizacaoId,
                    assinatura.getId(),
                    null,
                    valorOriginal,
                    cupomResult.getValorDesconto(),
                    valor,
                    dto.getPlanoCodigo(),
                    dto.getCicloCobranca()
            );
        }

        return toAssinaturaResponseDTO(assinatura);
    }

    // ==================== UPGRADE / DOWNGRADE ====================

    @Transactional
    public AssinaturaResponseDTO trocarPlano(EscolherPlanoDTO dto) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organizacao nao identificada no token");
        }

        Assinatura assinatura = assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalStateException("Assinatura nao encontrada"));

        if (assinatura.getStatus() != StatusAssinatura.ATIVA) {
            throw new IllegalStateException("Apenas assinaturas ativas podem trocar de plano");
        }

        PlanoBellory novoPlano = planoBelloryRepository.findByCodigo(dto.getPlanoCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Plano nao encontrado: " + dto.getPlanoCodigo()));

        if (!novoPlano.isAtivo()) {
            throw new IllegalArgumentException("Plano indisponivel");
        }

        PlanoBellory planoAtual = assinatura.getPlanoBellory();
        if (planoAtual.getCodigo().equals(novoPlano.getCodigo())
                && assinatura.getCicloCobranca().name().equals(dto.getCicloCobranca())) {
            throw new IllegalArgumentException("Voce ja esta neste plano com este ciclo de cobranca");
        }

        CicloCobranca novoCiclo = CicloCobranca.valueOf(dto.getCicloCobranca());
        FormaPagamentoPlataforma forma = FormaPagamentoPlataforma.valueOf(dto.getFormaPagamento());

        // Calculate pro-rata difference
        BigDecimal proRata = calcularProRata(assinatura, novoPlano, novoCiclo);

        int comparison = proRata.compareTo(BigDecimal.ZERO);

        if (comparison > 0) {
            // UPGRADE: pro-rata charge required before plan change
            return processarUpgrade(assinatura, novoPlano, novoCiclo, forma, proRata, dto, organizacaoId);
        } else if (comparison < 0) {
            // DOWNGRADE: credit the difference
            return processarDowngrade(assinatura, novoPlano, novoCiclo, forma, proRata.abs(), dto, organizacaoId);
        } else {
            // SAME PRICE: e.g. just changing cycle
            return processarTrocaMesmoValor(assinatura, novoPlano, novoCiclo, forma, dto, organizacaoId);
        }
    }

    private AssinaturaResponseDTO processarUpgrade(Assinatura assinatura, PlanoBellory novoPlano,
                                                    CicloCobranca novoCiclo, FormaPagamentoPlataforma forma,
                                                    BigDecimal proRata, EscolherPlanoDTO dto, Long organizacaoId) {
        PlanoBellory planoAtual = assinatura.getPlanoBellory();
        String planoAtualCodigo = planoAtual.getCodigo();

        // Generate avulsa charge in Asaas for the pro-rata amount
        if (assinatura.getAssasCustomerId() != null) {
            try {
                AssasPaymentResponse cobrancaAvulsa = assasClient.criarCobrancaAvulsa(
                        assinatura.getAssasCustomerId(),
                        proRata,
                        mapFormaPagamento(forma),
                        "Upgrade de plano Bellory - " + planoAtual.getNome() + " para " + novoPlano.getNome()
                );
                if (cobrancaAvulsa != null) {
                    assinatura.setCobrancaUpgradeAssasId(cobrancaAvulsa.getId());
                }
            } catch (AssasApiException e) {
                log.error("Erro ao criar cobranca avulsa para upgrade: {}", e.getMessage());
                throw new IllegalStateException("Erro ao processar upgrade. Tente novamente.");
            }
        }

        // Save current plan code for rollback, set planoBellory to target (optimistic)
        assinatura.setPlanoAnteriorCodigo(planoAtualCodigo);
        assinatura.setPlanoBellory(novoPlano);
        assinatura.setCicloCobranca(novoCiclo);
        assinatura.setFormaPagamento(forma);
        assinatura.setStatus(StatusAssinatura.UPGRADE_PENDENTE);

        assinaturaRepository.save(assinatura);

        log.info("Upgrade pendente de pagamento - org: {} - de {} para {} - pro-rata: {}",
                organizacaoId, planoAtualCodigo, novoPlano.getCodigo(), proRata);

        return toAssinaturaResponseDTO(assinatura);
    }

    private AssinaturaResponseDTO processarDowngrade(Assinatura assinatura, PlanoBellory novoPlano,
                                                      CicloCobranca novoCiclo, FormaPagamentoPlataforma forma,
                                                      BigDecimal credito, EscolherPlanoDTO dto, Long organizacaoId) {
        PlanoBellory planoAtual = assinatura.getPlanoBellory();
        String planoAtualCodigo = planoAtual.getCodigo();
        String oldSubscriptionId = assinatura.getAssasSubscriptionId();

        // Save credit for future use
        assinatura.setCreditoProRata(credito);

        // Calculate new subscription value with credit applied to first charge
        BigDecimal novoValor = novoCiclo == CicloCobranca.ANUAL ? novoPlano.getPrecoAnual() : novoPlano.getPrecoMensal();
        BigDecimal valorPrimeiraCobranca = novoValor.subtract(credito);
        if (valorPrimeiraCobranca.compareTo(BigDecimal.ZERO) < 0) {
            valorPrimeiraCobranca = BigDecimal.ZERO;
        }

        // Validar e aplicar cupom
        CupomValidacaoResult cupomResult = null;
        if (dto.getCodigoCupom() != null && !dto.getCodigoCupom().isBlank()) {
            cupomResult = cupomDescontoService.validarCupom(
                    dto.getCodigoCupom(), assinatura.getOrganizacao(), dto.getPlanoCodigo(), dto.getCicloCobranca(), valorPrimeiraCobranca);
            if (!cupomResult.isValido()) {
                throw new IllegalArgumentException("Cupom invalido: " + cupomResult.getMensagem());
            }
            valorPrimeiraCobranca = cupomResult.getValorComDesconto();
        }

        // Section 3.2: Create new subscription FIRST, then cancel old
        if (assinatura.getAssasCustomerId() != null) {
            BigDecimal valorAssas = novoValor;
            if (cupomResult != null && cupomResult.isValido()
                    && cupomResult.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.RECORRENTE) {
                valorAssas = novoValor.subtract(cupomDescontoService.calcularDesconto(cupomResult.getCupom(), novoValor));
            }

            String billingType = mapFormaPagamento(forma);
            String cycle = novoCiclo == CicloCobranca.ANUAL ? "YEARLY" : "MONTHLY";

            AssasSubscriptionResponse sub = assasClient.criarAssinatura(
                    AssasSubscriptionRequest.builder()
                            .customer(assinatura.getAssasCustomerId())
                            .billingType(billingType)
                            .value(valorAssas)
                            .cycle(cycle)
                            .nextDueDate(LocalDate.now().plusDays(1).toString())
                            .description("Assinatura Bellory - Plano " + novoPlano.getNome())
                            .build()
            );
            if (sub != null) {
                assinatura.setAssasSubscriptionId(sub.getId());
            }

            // Only THEN cancel old subscription (Section 3.2 - create before cancel)
            if (oldSubscriptionId != null) {
                try {
                    assasClient.cancelarAssinatura(oldSubscriptionId);
                } catch (AssasApiException e) {
                    log.warn("Erro ao cancelar assinatura antiga no Asaas: {}", e.getMessage());
                }
            }
        }

        // Update local subscription
        assinatura.setPlanoBellory(novoPlano);
        assinatura.setCicloCobranca(novoCiclo);
        assinatura.setFormaPagamento(forma);
        assinatura.setDtInicio(LocalDateTime.now());
        assinatura.setPlanoAnteriorCodigo(planoAtualCodigo);
        // Status stays ATIVA for downgrade

        if (cupomResult != null && cupomResult.isValido()) {
            assinatura.setCupom(cupomResult.getCupom());
            assinatura.setValorDesconto(cupomResult.getValorDesconto());
            assinatura.setCupomCodigo(cupomResult.getCupom().getCodigo());
        } else {
            assinatura.setCupom(null);
            assinatura.setValorDesconto(null);
            assinatura.setCupomCodigo(null);
        }

        if (novoCiclo == CicloCobranca.ANUAL) {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusYears(1));
        } else {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusMonths(1));
        }

        assinaturaRepository.save(assinatura);

        log.info("Downgrade realizado - org: {} - de {} para {} - credito pro-rata: {}",
                organizacaoId, planoAtualCodigo, novoPlano.getCodigo(), credito);

        return toAssinaturaResponseDTO(assinatura);
    }

    private AssinaturaResponseDTO processarTrocaMesmoValor(Assinatura assinatura, PlanoBellory novoPlano,
                                                            CicloCobranca novoCiclo, FormaPagamentoPlataforma forma,
                                                            EscolherPlanoDTO dto, Long organizacaoId) {
        PlanoBellory planoAtual = assinatura.getPlanoBellory();
        String planoAtualCodigo = planoAtual.getCodigo();
        String oldSubscriptionId = assinatura.getAssasSubscriptionId();

        BigDecimal novoValor = novoCiclo == CicloCobranca.ANUAL ? novoPlano.getPrecoAnual() : novoPlano.getPrecoMensal();

        // Validar e aplicar cupom
        CupomValidacaoResult cupomResult = null;
        if (dto.getCodigoCupom() != null && !dto.getCodigoCupom().isBlank()) {
            cupomResult = cupomDescontoService.validarCupom(
                    dto.getCodigoCupom(), assinatura.getOrganizacao(), dto.getPlanoCodigo(), dto.getCicloCobranca(), novoValor);
            if (!cupomResult.isValido()) {
                throw new IllegalArgumentException("Cupom invalido: " + cupomResult.getMensagem());
            }
        }

        // Create new subscription first, then cancel old (Section 3.2)
        if (assinatura.getAssasCustomerId() != null) {
            BigDecimal valorAssas = novoValor;
            if (cupomResult != null && cupomResult.isValido()
                    && cupomResult.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.RECORRENTE) {
                valorAssas = novoValor.subtract(cupomDescontoService.calcularDesconto(cupomResult.getCupom(), novoValor));
            }

            String billingType = mapFormaPagamento(forma);
            String cycle = novoCiclo == CicloCobranca.ANUAL ? "YEARLY" : "MONTHLY";

            AssasSubscriptionResponse sub = assasClient.criarAssinatura(
                    AssasSubscriptionRequest.builder()
                            .customer(assinatura.getAssasCustomerId())
                            .billingType(billingType)
                            .value(valorAssas)
                            .cycle(cycle)
                            .nextDueDate(LocalDate.now().plusDays(1).toString())
                            .description("Assinatura Bellory - Plano " + novoPlano.getNome())
                            .build()
            );
            if (sub != null) {
                assinatura.setAssasSubscriptionId(sub.getId());
            }

            // Cancel old subscription after creating new one
            if (oldSubscriptionId != null) {
                try {
                    assasClient.cancelarAssinatura(oldSubscriptionId);
                } catch (AssasApiException e) {
                    log.warn("Erro ao cancelar assinatura antiga no Asaas: {}", e.getMessage());
                }
            }
        }

        // Update local
        assinatura.setPlanoBellory(novoPlano);
        assinatura.setCicloCobranca(novoCiclo);
        assinatura.setFormaPagamento(forma);
        assinatura.setDtInicio(LocalDateTime.now());
        assinatura.setPlanoAnteriorCodigo(planoAtualCodigo);

        if (cupomResult != null && cupomResult.isValido()) {
            assinatura.setCupom(cupomResult.getCupom());
            assinatura.setValorDesconto(cupomResult.getValorDesconto());
            assinatura.setCupomCodigo(cupomResult.getCupom().getCodigo());
        } else {
            assinatura.setCupom(null);
            assinatura.setValorDesconto(null);
            assinatura.setCupomCodigo(null);
        }

        if (novoCiclo == CicloCobranca.ANUAL) {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusYears(1));
        } else {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusMonths(1));
        }

        assinaturaRepository.save(assinatura);

        log.info("Plano trocado (mesmo valor) - org: {} - de {} para {}",
                organizacaoId, planoAtualCodigo, novoPlano.getCodigo());

        return toAssinaturaResponseDTO(assinatura);
    }

    /**
     * Calculates the pro-rata difference when switching plans.
     * Positive = upgrade (customer owes money), Negative = downgrade (customer gets credit).
     */
    public BigDecimal calcularProRata(Assinatura assinatura, PlanoBellory novoPlano, CicloCobranca novoCiclo) {
        if (assinatura.getAssasSubscriptionId() == null) {
            return BigDecimal.ZERO;
        }

        AssasSubscriptionResponse sub = assasClient.buscarAssinatura(assinatura.getAssasSubscriptionId());
        if (sub == null || sub.getNextDueDate() == null) {
            return BigDecimal.ZERO;
        }

        LocalDate fimCiclo = LocalDate.parse(sub.getNextDueDate());
        LocalDate hoje = LocalDate.now();

        long diasRestantes = ChronoUnit.DAYS.between(hoje, fimCiclo);
        if (diasRestantes <= 0) {
            return BigDecimal.ZERO;
        }

        long diasTotalCiclo = assinatura.getCicloCobranca() == CicloCobranca.ANUAL ? 365 : fimCiclo.lengthOfMonth();

        BigDecimal valorAtual = assinatura.getCicloCobranca() == CicloCobranca.ANUAL
                ? assinatura.getPlanoBellory().getPrecoAnual()
                : assinatura.getPlanoBellory().getPrecoMensal();
        BigDecimal valorNovo = novoCiclo == CicloCobranca.ANUAL
                ? novoPlano.getPrecoAnual()
                : novoPlano.getPrecoMensal();

        BigDecimal fatorProporcional = BigDecimal.valueOf(diasRestantes)
                .divide(BigDecimal.valueOf(diasTotalCiclo), 4, RoundingMode.HALF_UP);

        BigDecimal creditoAtual = valorAtual.multiply(fatorProporcional).setScale(2, RoundingMode.HALF_UP);
        BigDecimal custoNovo = valorNovo.multiply(fatorProporcional).setScale(2, RoundingMode.HALF_UP);

        return custoNovo.subtract(creditoAtual);
    }

    // ==================== PREVIEW TROCA PLANO ====================

    @Transactional(readOnly = true)
    public ProRataPreviewDTO previewTrocaPlano(EscolherPlanoDTO dto) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organizacao nao identificada no token");
        }

        Assinatura assinatura = assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalStateException("Assinatura nao encontrada"));

        if (assinatura.getStatus() != StatusAssinatura.ATIVA) {
            throw new IllegalStateException("Apenas assinaturas ativas podem trocar de plano");
        }

        PlanoBellory novoPlano = planoBelloryRepository.findByCodigo(dto.getPlanoCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Plano nao encontrado: " + dto.getPlanoCodigo()));

        CicloCobranca novoCiclo = CicloCobranca.valueOf(dto.getCicloCobranca());
        BigDecimal proRata = calcularProRata(assinatura, novoPlano, novoCiclo);

        // Calculate proportional values for display
        long diasRestantes = 0;
        long diasTotalCiclo = 0;
        BigDecimal valorAtualProporcional = BigDecimal.ZERO;
        BigDecimal valorNovoProporcional = BigDecimal.ZERO;

        if (assinatura.getAssasSubscriptionId() != null) {
            try {
                AssasSubscriptionResponse sub = assasClient.buscarAssinatura(assinatura.getAssasSubscriptionId());
                if (sub != null && sub.getNextDueDate() != null) {
                    LocalDate fimCiclo = LocalDate.parse(sub.getNextDueDate());
                    diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), fimCiclo);
                    diasTotalCiclo = assinatura.getCicloCobranca() == CicloCobranca.ANUAL ? 365 : fimCiclo.lengthOfMonth();

                    if (diasRestantes > 0 && diasTotalCiclo > 0) {
                        BigDecimal fator = BigDecimal.valueOf(diasRestantes)
                                .divide(BigDecimal.valueOf(diasTotalCiclo), 4, RoundingMode.HALF_UP);
                        BigDecimal valorAtual = assinatura.getCicloCobranca() == CicloCobranca.ANUAL
                                ? assinatura.getPlanoBellory().getPrecoAnual()
                                : assinatura.getPlanoBellory().getPrecoMensal();
                        BigDecimal valorNovo = novoCiclo == CicloCobranca.ANUAL
                                ? novoPlano.getPrecoAnual() : novoPlano.getPrecoMensal();
                        valorAtualProporcional = valorAtual.multiply(fator).setScale(2, RoundingMode.HALF_UP);
                        valorNovoProporcional = valorNovo.multiply(fator).setScale(2, RoundingMode.HALF_UP);
                    }
                }
            } catch (AssasApiException e) {
                log.warn("Erro ao buscar dados do Asaas para preview: {}", e.getMessage());
            }
        }

        boolean isUpgrade = proRata.compareTo(BigDecimal.ZERO) > 0;
        String mensagem = isUpgrade
                ? String.format("Upgrade: sera cobrado R$ %.2f proporcional ao periodo restante.", proRata)
                : proRata.compareTo(BigDecimal.ZERO) < 0
                        ? String.format("Downgrade: voce recebera R$ %.2f de credito na proxima cobranca.", proRata.abs())
                        : "Troca sem diferenca de valor.";

        return ProRataPreviewDTO.builder()
                .planoAtualCodigo(assinatura.getPlanoBellory().getCodigo())
                .planoAtualNome(assinatura.getPlanoBellory().getNome())
                .novoPlanoCodigo(novoPlano.getCodigo())
                .novoPlanoNome(novoPlano.getNome())
                .cicloCobranca(novoCiclo.name())
                .valorAtualProporcional(valorAtualProporcional)
                .valorNovoProporcional(valorNovoProporcional)
                .valorProRata(proRata)
                .diasRestantesCiclo(diasRestantes)
                .diasTotalCiclo(diasTotalCiclo)
                .isUpgrade(isUpgrade)
                .mensagem(mensagem)
                .build();
    }

    // ==================== CANCELAMENTO ====================

    @Transactional
    public AssinaturaResponseDTO cancelarAssinatura() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organizacao nao identificada no token");
        }

        Assinatura assinatura = assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalStateException("Assinatura nao encontrada"));

        if (assinatura.getStatus() != StatusAssinatura.ATIVA) {
            throw new IllegalStateException("Apenas assinaturas ativas podem ser canceladas");
        }

        // Cancelar no Asaas
        if (assinatura.getAssasSubscriptionId() != null) {
            try {
                assasClient.cancelarAssinatura(assinatura.getAssasSubscriptionId());
            } catch (AssasApiException e) {
                log.warn("Erro ao cancelar assinatura no Asaas: {}", e.getMessage());
            }
        }

        assinatura.setStatus(StatusAssinatura.CANCELADA);
        assinatura.setDtCancelamento(LocalDateTime.now());
        assinaturaRepository.save(assinatura);

        // Cancelar cobrancas pendentes locais
        List<CobrancaPlataforma> cobrancasPendentes = cobrancaPlataformaRepository
                .findByOrganizacaoIdAndStatus(organizacaoId, StatusCobrancaPlataforma.PENDENTE);
        for (CobrancaPlataforma cobranca : cobrancasPendentes) {
            cobranca.setStatus(StatusCobrancaPlataforma.CANCELADA);
            cobrancaPlataformaRepository.save(cobranca);
        }

        log.info("Assinatura cancelada para organizacao ID: {} - acesso ate: {}",
                organizacaoId, assinatura.getDtProximoVencimento());

        return toAssinaturaResponseDTO(assinatura);
    }

    @Transactional
    public AssinaturaResponseDTO reativarAssinatura(EscolherPlanoDTO dto) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organizacao nao identificada no token");
        }

        Assinatura assinatura = assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalStateException("Assinatura nao encontrada"));

        if (assinatura.getStatus() != StatusAssinatura.CANCELADA
                && assinatura.getStatus() != StatusAssinatura.VENCIDA) {
            throw new IllegalStateException("Apenas assinaturas canceladas ou vencidas podem ser reativadas");
        }

        PlanoBellory plano = planoBelloryRepository.findByCodigo(dto.getPlanoCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Plano nao encontrado: " + dto.getPlanoCodigo()));

        if (!plano.isAtivo()) {
            throw new IllegalArgumentException("Plano indisponivel");
        }

        CicloCobranca ciclo = CicloCobranca.valueOf(dto.getCicloCobranca());
        FormaPagamentoPlataforma forma = FormaPagamentoPlataforma.valueOf(dto.getFormaPagamento());

        BigDecimal valorOriginal = ciclo == CicloCobranca.ANUAL ? plano.getPrecoAnual() : plano.getPrecoMensal();
        BigDecimal valor = valorOriginal;

        // Validar e aplicar cupom
        CupomValidacaoResult cupomResult = null;
        if (dto.getCodigoCupom() != null && !dto.getCodigoCupom().isBlank()) {
            cupomResult = cupomDescontoService.validarCupom(
                    dto.getCodigoCupom(), assinatura.getOrganizacao(), dto.getPlanoCodigo(), dto.getCicloCobranca(), valorOriginal);
            if (!cupomResult.isValido()) {
                throw new IllegalArgumentException("Cupom invalido: " + cupomResult.getMensagem());
            }
            valor = cupomResult.getValorComDesconto();
        }

        // Criar nova assinatura no Asaas
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

        assinatura.setPlanoBellory(plano);
        assinatura.setCicloCobranca(ciclo);
        assinatura.setFormaPagamento(forma);
        assinatura.setDtInicio(LocalDateTime.now());
        assinatura.setDtCancelamento(null);

        // Set status based on payment method
        if (forma == FormaPagamentoPlataforma.CARTAO_CREDITO) {
            assinatura.setStatus(StatusAssinatura.ATIVA);
        } else {
            // Boleto/PIX: wait for webhook confirmation
            assinatura.setStatus(StatusAssinatura.AGUARDANDO_PAGAMENTO);
        }

        if (cupomResult != null && cupomResult.isValido()) {
            assinatura.setCupom(cupomResult.getCupom());
            assinatura.setValorDesconto(cupomResult.getValorDesconto());
            assinatura.setCupomCodigo(cupomResult.getCupom().getCodigo());
        } else {
            assinatura.setCupom(null);
            assinatura.setValorDesconto(null);
            assinatura.setCupomCodigo(null);
        }

        if (ciclo == CicloCobranca.ANUAL) {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusYears(1));
        } else {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusMonths(1));
        }

        assinaturaRepository.save(assinatura);

        if (cupomResult != null && cupomResult.isValido()) {
            cupomDescontoService.registrarUtilizacao(
                    cupomResult.getCupom(),
                    organizacaoId,
                    assinatura.getId(),
                    null,
                    valorOriginal,
                    cupomResult.getValorDesconto(),
                    valor,
                    dto.getPlanoCodigo(),
                    dto.getCicloCobranca()
            );
        }

        log.info("Assinatura reativada para organizacao ID: {} - plano: {} - ciclo: {}",
                organizacaoId, plano.getCodigo(), ciclo);

        return toAssinaturaResponseDTO(assinatura);
    }

    private String mapFormaPagamento(FormaPagamentoPlataforma forma) {
        return switch (forma) {
            case PIX -> "PIX";
            case BOLETO -> "BOLETO";
            case CARTAO_CREDITO -> "CREDIT_CARD";
        };
    }

    // ==================== VALIDAR CUPOM ====================

    @Transactional(readOnly = true)
    public CupomValidacaoResponseDTO validarCupomParaOrganizacao(ValidarCupomDTO dto) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organizacao nao identificada no token");
        }

        Organizacao org = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalStateException("Organizacao nao encontrada"));

        PlanoBellory plano = planoBelloryRepository.findByCodigo(dto.getPlanoCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Plano nao encontrado: " + dto.getPlanoCodigo()));

        CicloCobranca ciclo = CicloCobranca.valueOf(dto.getCicloCobranca());
        BigDecimal valorOriginal = ciclo == CicloCobranca.ANUAL ? plano.getPrecoAnual() : plano.getPrecoMensal();

        CupomValidacaoResult result = cupomDescontoService.validarCupom(
                dto.getCodigoCupom(), org, dto.getPlanoCodigo(), dto.getCicloCobranca(), valorOriginal);

        return CupomValidacaoResponseDTO.builder()
                .valido(result.isValido())
                .mensagem(result.getMensagem())
                .tipoDesconto(result.getCupom() != null ? result.getCupom().getTipoDesconto().name() : null)
                .tipoAplicacao(result.getCupom() != null ? result.getCupom().getTipoAplicacao().name() : null)
                .percentualDesconto(result.getCupom() != null ? result.getCupom().getValorDesconto() : null)
                .valorDesconto(result.getValorDesconto())
                .valorOriginal(result.getValorOriginal())
                .valorComDesconto(result.getValorComDesconto())
                .build();
    }

    // ==================== COBRANCAS (consulta Asaas) ====================

    @Transactional(readOnly = true)
    public List<CobrancaPlataformaDTO> getMinhasCobrancas(String statusFiltro) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organizacao nao identificada no token");
        }

        // Tentar buscar do Asaas primeiro
        Assinatura assinatura = assinaturaRepository.findByOrganizacaoId(organizacaoId).orElse(null);
        if (assinatura != null && assinatura.getAssasSubscriptionId() != null) {
            try {
                AssasPaymentListResponse assasPayments = assasClient.buscarPagamentosAssinatura(assinatura.getAssasSubscriptionId());
                if (assasPayments != null && assasPayments.getData() != null) {
                    return assasPayments.getData().stream()
                            .filter(p -> statusFiltro == null || statusFiltro.isBlank() || matchStatus(p.getStatus(), statusFiltro))
                            .map(this::toCobrancaDTOFromAsaas)
                            .collect(Collectors.toList());
                }
            } catch (AssasApiException e) {
                log.warn("Erro ao buscar cobrancas do Asaas, usando dados locais: {}", e.getMessage());
            }
        }

        // Fallback: dados locais
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

    private boolean matchStatus(String assasStatus, String localStatus) {
        return switch (localStatus.toUpperCase()) {
            case "PAGA" -> "RECEIVED".equals(assasStatus) || "CONFIRMED".equals(assasStatus) || "RECEIVED_IN_CASH".equals(assasStatus);
            case "PENDENTE" -> "PENDING".equals(assasStatus);
            case "VENCIDA" -> "OVERDUE".equals(assasStatus);
            case "CANCELADA" -> "DELETED".equals(assasStatus) || "REFUNDED".equals(assasStatus);
            default -> true;
        };
    }

    private CobrancaPlataformaDTO toCobrancaDTOFromAsaas(AssasPaymentResponse payment) {
        String statusLocal = switch (payment.getStatus()) {
            case "PENDING" -> "PENDENTE";
            case "RECEIVED", "CONFIRMED", "RECEIVED_IN_CASH" -> "PAGA";
            case "OVERDUE" -> "VENCIDA";
            case "DELETED", "REFUNDED" -> "CANCELADA";
            default -> payment.getStatus();
        };

        // Mapear billingType do Asaas para FormaPagamentoPlataforma local
        String formaPagamentoLocal = switch (payment.getBillingType() != null ? payment.getBillingType() : "") {
            case "PIX" -> "PIX";
            case "BOLETO" -> "BOLETO";
            case "CREDIT_CARD" -> "CARTAO_CREDITO";
            default -> payment.getBillingType();
        };

        // Extrair mes/ano de referencia do dueDate
        LocalDate dtVencimento = payment.getDueDate() != null ? LocalDate.parse(payment.getDueDate()) : null;
        Integer referenciaMes = dtVencimento != null ? dtVencimento.getMonthValue() : null;
        Integer referenciaAno = dtVencimento != null ? dtVencimento.getYear() : null;

        // Mapear data de pagamento
        LocalDateTime dtPagamento = null;
        if (payment.getPaymentDate() != null && !payment.getPaymentDate().isBlank()) {
            dtPagamento = LocalDate.parse(payment.getPaymentDate()).atStartOfDay();
        }

        // Mapear data de criacao
        LocalDateTime dtCriacao = null;
        if (payment.getDateCreated() != null && !payment.getDateCreated().isBlank()) {
            dtCriacao = LocalDate.parse(payment.getDateCreated()).atStartOfDay();
        }

        // Buscar PIX QR Code para pagamentos PIX pendentes/vencidos
        String pixQrCode = null;
        String pixCopiaCola = null;
        String pixExpirationDate = null;
        if ("PIX".equals(formaPagamentoLocal) && ("PENDING".equals(payment.getStatus()) || "OVERDUE".equals(payment.getStatus()))) {
            try {
                var pixData = assasClient.buscarPixQrCode(payment.getId());
                if (pixData != null) {
                    pixQrCode = pixData.getEncodedImage();
                    pixCopiaCola = pixData.getPayload();
                    pixExpirationDate = pixData.getExpirationDate();
                }
            } catch (Exception e) {
                log.warn("Erro ao buscar PIX QR Code para payment {}: {}", payment.getId(), e.getMessage());
            }
        }

        // Enriquecer com dados locais (cupom, valor original, etc.)
        CobrancaPlataformaDTO.CobrancaPlataformaDTOBuilder builder = CobrancaPlataformaDTO.builder()
                .valor(payment.getValue())
                .dtVencimento(dtVencimento)
                .dtPagamento(dtPagamento)
                .status(statusLocal)
                .formaPagamento(formaPagamentoLocal)
                .assasInvoiceUrl(payment.getInvoiceUrl())
                .assasBankSlipUrl(payment.getBankSlipUrl())
                .assasPixQrCode(pixQrCode)
                .assasPixCopiaCola(pixCopiaCola)
                .assasPixExpirationDate(pixExpirationDate)
                .referenciaMes(referenciaMes)
                .referenciaAno(referenciaAno)
                .dtCriacao(dtCriacao);

        // Tentar encontrar cobranca local correspondente para pegar id, cupom e valores originais
        CobrancaPlataforma cobrancaLocal = cobrancaPlataformaRepository.findByAssasPaymentId(payment.getId()).orElse(null);
        if (cobrancaLocal != null) {
            builder.id(cobrancaLocal.getId())
                    .cupomCodigo(cobrancaLocal.getCupomCodigo())
                    .valorOriginal(cobrancaLocal.getValorOriginal())
                    .valorDescontoAplicado(cobrancaLocal.getValorDescontoAplicado());
        }

        return builder.build();
    }

    // ==================== WEBHOOK PAGAMENTO ====================

    @Transactional
    public void processarWebhookPagamento(AssasWebhookPayload payload) {
        if (payload == null || payload.getPayment() == null) {
            log.warn("Webhook Asaas recebido com payload invalido");
            return;
        }

        String event = payload.getEvent();
        AssasWebhookPayload.Payment payment = payload.getPayment();
        log.info("Webhook Asaas recebido - evento: {}, paymentId: {}, subscription: {}", event, payment.getId(), payment.getSubscription());

        // Idempotencia: verificar se ja processou este evento para este payment
        if (webhookLogRepository.existsByAssasPaymentIdAndEvento(payment.getId(), event)) {
            log.info("Webhook ja processado (idempotente): paymentId={}, evento={}", payment.getId(), event);
            return;
        }

        // Buscar assinatura pelo subscription ID do Asaas
        Assinatura assinatura = null;
        if (payment.getSubscription() != null) {
            assinatura = assinaturaRepository.findByAssasSubscriptionId(payment.getSubscription()).orElse(null);
        }

        // Fallback: buscar pela cobranca com assas_payment_id
        if (assinatura == null) {
            assinatura = cobrancaPlataformaRepository.findByAssasPaymentId(payment.getId())
                    .map(CobrancaPlataforma::getAssinatura)
                    .orElse(null);
        }

        // Also check if this is a pro-rata upgrade charge (not linked to a subscription)
        if (assinatura == null && payment.getSubscription() == null) {
            // Search for assinatura with this cobrancaUpgradeAssasId
            assinatura = assinaturaRepository.findAll().stream()
                    .filter(a -> payment.getId().equals(a.getCobrancaUpgradeAssasId()))
                    .findFirst()
                    .orElse(null);
        }

        // Registrar log do webhook
        WebhookLog webhookLog = WebhookLog.builder()
                .assinatura(assinatura)
                .evento(event)
                .assasPaymentId(payment.getId())
                .assasSubscriptionId(payment.getSubscription())
                .valor(payment.getValue())
                .statusPagamento(payment.getStatus())
                .payloadResumo(String.format("billingType=%s, dueDate=%s", payment.getBillingType(), payment.getDueDate()))
                .build();
        webhookLogRepository.save(webhookLog);

        // Evictar caches do Asaas
        if (payment.getSubscription() != null) {
            assasClient.evictSubscriptionCache(payment.getSubscription());
            assasClient.evictPaymentsCache(payment.getSubscription());
        }

        // Processar evento
        switch (event) {
            case "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED" -> confirmarPagamento(payment, assinatura);
            case "PAYMENT_OVERDUE" -> marcarPagamentoAtrasado(payment, assinatura);
            case "PAYMENT_REFUNDED", "PAYMENT_REFUND_IN_PROGRESS" -> processarEstorno(payment, assinatura);
            case "PAYMENT_DELETED" -> log.info("Pagamento deletado no Asaas: {}", payment.getId());
            case "PAYMENT_RESTORED" -> restaurarPagamento(payment, assinatura);
            default -> log.info("Evento Asaas nao tratado: {}", event);
        }
    }

    // ==================== WEBHOOK ASSINATURA ====================

    @Transactional
    public void processarWebhookAssinatura(AssasWebhookPayload payload) {
        if (payload == null) {
            log.warn("Webhook Asaas de assinatura recebido com payload invalido");
            return;
        }

        String event = payload.getEvent();
        // For subscription events, the payload may contain subscription info in the payment field
        // or we extract the subscription ID from the event
        AssasWebhookPayload.Payment paymentData = payload.getPayment();
        if (paymentData == null) {
            log.warn("Webhook de assinatura sem dados de payment: {}", event);
            return;
        }

        String subscriptionId = paymentData.getSubscription();
        if (subscriptionId == null) {
            subscriptionId = paymentData.getId(); // Some subscription events may use id
        }

        log.info("Webhook Asaas de assinatura - evento: {}, subscriptionId: {}", event, subscriptionId);

        Assinatura assinatura = assinaturaRepository.findByAssasSubscriptionId(subscriptionId).orElse(null);
        if (assinatura == null) {
            log.warn("Assinatura nao encontrada para subscriptionId: {}", subscriptionId);
            return;
        }

        // Registrar log
        WebhookLog webhookLog = WebhookLog.builder()
                .assinatura(assinatura)
                .evento(event)
                .assasSubscriptionId(subscriptionId)
                .payloadResumo("subscription_event=" + event)
                .build();
        webhookLogRepository.save(webhookLog);

        switch (event) {
            case "SUBSCRIPTION_RENEWED" -> {
                log.info("Assinatura renovada no Asaas - org: {}", assinatura.getOrganizacao().getId());
                // Update next due date from Asaas
                try {
                    AssasSubscriptionResponse sub = assasClient.buscarAssinatura(subscriptionId);
                    if (sub != null && sub.getNextDueDate() != null) {
                        LocalDate nextDue = LocalDate.parse(sub.getNextDueDate());
                        assinatura.setDtProximoVencimento(nextDue.atStartOfDay());
                        assinaturaRepository.save(assinatura);
                    }
                } catch (AssasApiException e) {
                    log.warn("Erro ao buscar assinatura renovada no Asaas: {}", e.getMessage());
                }
            }
            case "SUBSCRIPTION_EXPIRED" -> {
                if (assinatura.getStatus() == StatusAssinatura.ATIVA) {
                    assinatura.setStatus(StatusAssinatura.VENCIDA);
                    assinaturaRepository.save(assinatura);
                    log.info("Assinatura expirada no Asaas - org marcada como VENCIDA: {}",
                            assinatura.getOrganizacao().getId());
                }
            }
            case "SUBSCRIPTION_DELETED" -> {
                log.info("Assinatura deletada no Asaas - org: {}", assinatura.getOrganizacao().getId());
                // If status is still ATIVA, mark as CANCELADA
                if (assinatura.getStatus() == StatusAssinatura.ATIVA) {
                    assinatura.setStatus(StatusAssinatura.CANCELADA);
                    assinatura.setDtCancelamento(LocalDateTime.now());
                    assinaturaRepository.save(assinatura);
                }
            }
            case "SUBSCRIPTION_UPDATED" -> {
                log.info("Assinatura atualizada no Asaas - org: {}", assinatura.getOrganizacao().getId());
                // Sync next due date
                try {
                    AssasSubscriptionResponse sub = assasClient.buscarAssinatura(subscriptionId);
                    if (sub != null && sub.getNextDueDate() != null) {
                        LocalDate nextDue = LocalDate.parse(sub.getNextDueDate());
                        assinatura.setDtProximoVencimento(nextDue.atStartOfDay());
                        assinaturaRepository.save(assinatura);
                    }
                } catch (AssasApiException e) {
                    log.warn("Erro ao sincronizar assinatura atualizada: {}", e.getMessage());
                }
            }
            default -> log.info("Evento de assinatura Asaas nao tratado: {}", event);
        }
    }

    private void confirmarPagamento(AssasWebhookPayload.Payment payment, Assinatura assinatura) {
        if (assinatura == null) {
            log.warn("Assinatura nao encontrada para pagamento confirmado: {}", payment.getId());
            return;
        }

        // Check if this is a pro-rata upgrade charge
        if (assinatura.getCobrancaUpgradeAssasId() != null
                && assinatura.getCobrancaUpgradeAssasId().equals(payment.getId())) {
            // Pro-rata paid - now execute the plan change
            efetivarUpgrade(assinatura);
            return; // Don't process as regular payment
        }

        // Atualizar cobranca local se existir
        cobrancaPlataformaRepository.findByAssasPaymentId(payment.getId())
                .ifPresent(cobranca -> {
                    cobranca.setStatus(StatusCobrancaPlataforma.PAGA);
                    cobranca.setDtPagamento(LocalDateTime.now());
                    cobrancaPlataformaRepository.save(cobranca);

                    PagamentoPlataforma pagamento = PagamentoPlataforma.builder()
                            .cobranca(cobranca)
                            .valor(payment.getValue())
                            .status(StatusPagamentoPlataforma.CONFIRMADO)
                            .formaPagamento(cobranca.getFormaPagamento())
                            .assasPaymentId(payment.getId())
                            .dtPagamento(LocalDateTime.now())
                            .build();
                    pagamentoPlataformaRepository.save(pagamento);
                });

        // Handle AGUARDANDO_PAGAMENTO -> ATIVA and VENCIDA -> ATIVA transitions
        if (assinatura.getStatus() == StatusAssinatura.VENCIDA
                || assinatura.getStatus() == StatusAssinatura.AGUARDANDO_PAGAMENTO) {
            assinatura.setStatus(StatusAssinatura.ATIVA);
        }

        // Get nextDueDate from Asaas (Section 3.5)
        if (payment.getSubscription() != null) {
            try {
                AssasSubscriptionResponse sub = assasClient.buscarAssinatura(payment.getSubscription());
                if (sub != null && sub.getNextDueDate() != null) {
                    LocalDate nextDue = LocalDate.parse(sub.getNextDueDate());
                    assinatura.setDtProximoVencimento(nextDue.atStartOfDay());
                }
            } catch (AssasApiException e) {
                log.warn("Could not get nextDueDate from Asaas, using fallback calculation");
                if (payment.getDueDate() != null) {
                    LocalDate dueDate = LocalDate.parse(payment.getDueDate());
                    LocalDate nextDue = assinatura.getCicloCobranca() == CicloCobranca.ANUAL
                            ? dueDate.plusYears(1) : dueDate.plusMonths(1);
                    assinatura.setDtProximoVencimento(nextDue.atStartOfDay());
                }
            }
        }

        assinaturaRepository.save(assinatura);

        // Se cupom era PRIMEIRA_COBRANCA, atualizar Asaas para valor cheio
        if (assinatura.getCupom() != null
                && assinatura.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.PRIMEIRA_COBRANCA
                && assinatura.getAssasSubscriptionId() != null) {
            BigDecimal valorCheio = assinatura.getCicloCobranca() == CicloCobranca.ANUAL
                    ? assinatura.getPlanoBellory().getPrecoAnual() : assinatura.getPlanoBellory().getPrecoMensal();
            try {
                assasClient.atualizarAssinatura(
                        assinatura.getAssasSubscriptionId(),
                        AssasSubscriptionRequest.builder()
                                .value(valorCheio)
                                .build()
                );
            } catch (AssasApiException e) {
                log.error("Erro ao atualizar assinatura no Asaas para valor cheio: {}", e.getMessage());
            }
            assinatura.setCupom(null);
            assinatura.setValorDesconto(null);
            assinatura.setCupomCodigo(null);
            assinaturaRepository.save(assinatura);
            log.info("Cupom PRIMEIRA_COBRANCA removido - Asaas atualizado para valor cheio: {} - org: {}",
                    valorCheio, assinatura.getOrganizacao().getId());
        }

        // Section 3.7: Check RECORRENTE coupon expiration
        if (assinatura.getCupom() != null
                && assinatura.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.RECORRENTE
                && assinatura.getCupom().getMesesRecorrencia() != null) {
            // Count paid charges with this coupon
            long totalUtilizacoes = cupomUtilizacaoRepository.countByCupomIdAndOrganizacaoId(
                    assinatura.getCupom().getId(), assinatura.getOrganizacao().getId());
            if (totalUtilizacoes >= assinatura.getCupom().getMesesRecorrencia()) {
                // Coupon recurrence exhausted - update Asaas to full price and clear coupon
                BigDecimal valorCheio = assinatura.getCicloCobranca() == CicloCobranca.ANUAL
                        ? assinatura.getPlanoBellory().getPrecoAnual() : assinatura.getPlanoBellory().getPrecoMensal();
                try {
                    assasClient.atualizarAssinatura(
                            assinatura.getAssasSubscriptionId(),
                            AssasSubscriptionRequest.builder()
                                    .value(valorCheio)
                                    .build()
                    );
                } catch (AssasApiException e) {
                    log.error("Erro ao atualizar assinatura no Asaas para valor cheio apos cupom recorrente: {}", e.getMessage());
                }
                assinatura.setCupom(null);
                assinatura.setValorDesconto(null);
                assinatura.setCupomCodigo(null);
                assinaturaRepository.save(assinatura);
                log.info("Cupom RECORRENTE expirado (meses: {}) - Asaas atualizado para valor cheio: {} - org: {}",
                        totalUtilizacoes, valorCheio, assinatura.getOrganizacao().getId());
            }
        }

        log.info("Pagamento confirmado - assinatura org: {}", assinatura.getOrganizacao().getId());
    }

    /**
     * Executes the upgrade after the pro-rata charge is confirmed.
     * At this point, planoBellory is already set to the new plan (optimistic set during trocarPlano).
     * We need to create the new Asaas subscription and cancel the old one.
     */
    private void efetivarUpgrade(Assinatura assinatura) {
        log.info("Efetivando upgrade para org: {} - plano: {}",
                assinatura.getOrganizacao().getId(), assinatura.getPlanoBellory().getCodigo());

        String oldSubscriptionId = assinatura.getAssasSubscriptionId();
        PlanoBellory novoPlano = assinatura.getPlanoBellory();
        CicloCobranca ciclo = assinatura.getCicloCobranca();
        FormaPagamentoPlataforma forma = assinatura.getFormaPagamento();

        BigDecimal novoValor = ciclo == CicloCobranca.ANUAL ? novoPlano.getPrecoAnual() : novoPlano.getPrecoMensal();

        // Create new subscription in Asaas FIRST (Section 3.2)
        if (assinatura.getAssasCustomerId() != null) {
            String billingType = mapFormaPagamento(forma);
            String cycle = ciclo == CicloCobranca.ANUAL ? "YEARLY" : "MONTHLY";

            AssasSubscriptionResponse sub = assasClient.criarAssinatura(
                    AssasSubscriptionRequest.builder()
                            .customer(assinatura.getAssasCustomerId())
                            .billingType(billingType)
                            .value(novoValor)
                            .cycle(cycle)
                            .nextDueDate(LocalDate.now().plusDays(1).toString())
                            .description("Assinatura Bellory - Plano " + novoPlano.getNome())
                            .build()
            );
            if (sub != null) {
                assinatura.setAssasSubscriptionId(sub.getId());
            }

            // Then cancel old subscription
            if (oldSubscriptionId != null) {
                try {
                    assasClient.cancelarAssinatura(oldSubscriptionId);
                } catch (AssasApiException e) {
                    log.warn("Erro ao cancelar assinatura antiga no Asaas durante upgrade: {}", e.getMessage());
                }
            }
        }

        // Update local state
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setCobrancaUpgradeAssasId(null);
        assinatura.setDtInicio(LocalDateTime.now());

        if (ciclo == CicloCobranca.ANUAL) {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusYears(1));
        } else {
            assinatura.setDtProximoVencimento(LocalDateTime.now().plusMonths(1));
        }

        assinaturaRepository.save(assinatura);

        log.info("Upgrade efetivado - org: {} - novo plano: {}",
                assinatura.getOrganizacao().getId(), novoPlano.getCodigo());
    }

    private void marcarPagamentoAtrasado(AssasWebhookPayload.Payment payment, Assinatura assinatura) {
        // Atualizar cobranca local se existir
        cobrancaPlataformaRepository.findByAssasPaymentId(payment.getId())
                .ifPresent(cobranca -> {
                    cobranca.setStatus(StatusCobrancaPlataforma.VENCIDA);
                    cobrancaPlataformaRepository.save(cobranca);
                });

        if (assinatura != null) {
            // Handle UPGRADE_PENDENTE: if the pro-rata charge is overdue, rollback
            if (assinatura.getStatus() == StatusAssinatura.UPGRADE_PENDENTE
                    && assinatura.getCobrancaUpgradeAssasId() != null
                    && assinatura.getCobrancaUpgradeAssasId().equals(payment.getId())) {
                // Rollback to previous plan
                if (assinatura.getPlanoAnteriorCodigo() != null) {
                    planoBelloryRepository.findByCodigo(assinatura.getPlanoAnteriorCodigo())
                            .ifPresent(assinatura::setPlanoBellory);
                }
                assinatura.setStatus(StatusAssinatura.ATIVA);
                assinatura.setCobrancaUpgradeAssasId(null);
                assinatura.setPlanoAnteriorCodigo(null);
                assinaturaRepository.save(assinatura);
                log.info("Upgrade cancelado por pagamento atrasado - rollback para plano anterior - org: {}",
                        assinatura.getOrganizacao().getId());
                return;
            }

            if (assinatura.getStatus() == StatusAssinatura.ATIVA) {
                assinatura.setStatus(StatusAssinatura.VENCIDA);
                assinaturaRepository.save(assinatura);
                log.info("Assinatura marcada como vencida por pagamento atrasado - org: {}", assinatura.getOrganizacao().getId());
            }
        }
    }

    private void processarEstorno(AssasWebhookPayload.Payment payment, Assinatura assinatura) {
        cobrancaPlataformaRepository.findByAssasPaymentId(payment.getId())
                .ifPresent(cobranca -> {
                    cobranca.setStatus(StatusCobrancaPlataforma.ESTORNADA);
                    cobrancaPlataformaRepository.save(cobranca);
                });
        log.info("Pagamento estornado: {}", payment.getId());
    }

    private void restaurarPagamento(AssasWebhookPayload.Payment payment, Assinatura assinatura) {
        if (assinatura != null && assinatura.getStatus() == StatusAssinatura.VENCIDA) {
            assinatura.setStatus(StatusAssinatura.ATIVA);
            assinaturaRepository.save(assinatura);
            log.info("Pagamento restaurado - assinatura reativada - org: {}", assinatura.getOrganizacao().getId());
        }
    }

    // ==================== SCHEDULER (mantidos) ====================

    @Transactional
    public void expirarTrials() {
        List<Assinatura> trialsExpirados = assinaturaRepository.findTrialsExpirados(LocalDateTime.now());

        PlanoBellory planoGratuito = planoBelloryRepository.findByCodigo(PLANO_GRATUITO_CODIGO)
                .orElse(null);

        for (Assinatura assinatura : trialsExpirados) {
            if (planoGratuito != null) {
                assinatura.setPlanoBellory(planoGratuito);
                assinatura.setStatus(StatusAssinatura.ATIVA);
                assinatura.setDtInicio(LocalDateTime.now());
                assinaturaRepository.save(assinatura);
                log.info("Trial expirado - migrado para plano gratuito - organizacao ID: {}",
                        assinatura.getOrganizacao().getId());
            } else {
                assinatura.setStatus(StatusAssinatura.VENCIDA);
                assinaturaRepository.save(assinatura);
                log.warn("Trial expirado - plano gratuito nao encontrado - organizacao bloqueada ID: {}",
                        assinatura.getOrganizacao().getId());
            }
        }
        if (!trialsExpirados.isEmpty()) {
            log.info("Total de trials processados: {}", trialsExpirados.size());
        }
    }

    @Transactional
    public void notificarTrialsExpirando(int diasAntes) {
        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fim = LocalDateTime.now().plusDays(diasAntes);

        List<Assinatura> trialsExpirando = assinaturaRepository.findTrialsExpirandoEntre(inicio, fim);
        int enviados = 0;

        for (Assinatura assinatura : trialsExpirando) {
            if (assinatura.isPlanoGratuito()) continue;
            if (assinatura.getDtTrialNotificado() != null) continue;

            try {
                enviarEmailTrialExpirando(assinatura);
                assinatura.setDtTrialNotificado(LocalDateTime.now());
                assinaturaRepository.save(assinatura);
                enviados++;
            } catch (Exception e) {
                log.error("Erro ao enviar email de trial expirando para organizacao ID: {} - {}",
                        assinatura.getOrganizacao().getId(), e.getMessage());
            }
        }

        if (enviados > 0) {
            log.info("Emails de aviso de trial expirando enviados: {}", enviados);
        }
    }

    private void enviarEmailTrialExpirando(Assinatura assinatura) {
        Organizacao org = assinatura.getOrganizacao();
        PlanoBellory plano = assinatura.getPlanoBellory();
        long diasRestantes = ChronoUnit.DAYS.between(LocalDateTime.now(), assinatura.getDtFimTrial());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        BigDecimal valor = assinatura.getCicloCobranca() == CicloCobranca.ANUAL
                ? plano.getPrecoAnual() : plano.getPrecoMensal();

        String cicloTexto = assinatura.getCicloCobranca() == CicloCobranca.ANUAL ? "Anual" : "Mensal";
        String formaTexto = assinatura.getFormaPagamento() != null
                ? switch (assinatura.getFormaPagamento()) {
                    case PIX -> "PIX";
                    case BOLETO -> "Boleto";
                    case CARTAO_CREDITO -> "Cartao de Credito";
                  }
                : "PIX";

        Map<String, Object> variables = new HashMap<>();
        variables.put("nomeOrganizacao", org.getNomeFantasia());
        variables.put("diasRestantes", Math.max(diasRestantes, 0));
        variables.put("dataExpiracao", assinatura.getDtFimTrial().format(formatter));
        variables.put("planoNome", plano.getNome());
        variables.put("cicloCobranca", cicloTexto);
        variables.put("valorPlano", currencyFormat.format(valor));
        variables.put("formaPagamento", formaTexto);
        variables.put("urlSistema", appUrl);

        emailService.enviarEmailComTemplate(
                List.of(org.getEmailPrincipal()),
                EmailTemplate.TRIAL_EXPIRANDO,
                variables
        );

        log.info("Email de trial expirando enviado para organizacao ID: {} - {} dias restantes",
                org.getId(), diasRestantes);
    }

    @Transactional
    public void bloquearCancelamentoExpirado() {
        List<Assinatura> canceladas = assinaturaRepository.findByStatus(StatusAssinatura.CANCELADA);
        PlanoBellory planoGratuito = planoBelloryRepository.findByCodigo(PLANO_GRATUITO_CODIGO)
                .orElse(null);

        for (Assinatura assinatura : canceladas) {
            if (assinatura.getDtProximoVencimento() != null
                    && LocalDateTime.now().isAfter(assinatura.getDtProximoVencimento())) {
                if (planoGratuito != null) {
                    assinatura.setPlanoBellory(planoGratuito);
                    assinatura.setStatus(StatusAssinatura.ATIVA);
                    assinatura.setAssasSubscriptionId(null);
                    assinatura.setCupom(null);
                    assinatura.setValorDesconto(null);
                    assinatura.setCupomCodigo(null);
                    assinaturaRepository.save(assinatura);
                    log.info("Cancelamento expirado - migrado para plano gratuito - organizacao ID: {}",
                            assinatura.getOrganizacao().getId());
                }
            }
        }
    }

    // ==================== SINCRONIZACAO COM ASAAS ====================

    @Transactional
    public void sincronizarComAsaas() {
        if (!assasClient.isConfigurado()) {
            log.debug("Asaas nao configurado - sincronizacao ignorada");
            return;
        }

        List<Assinatura> assinaturas = assinaturaRepository.findByAssasSubscriptionIdNotNullAndStatusIn(
                List.of(StatusAssinatura.ATIVA, StatusAssinatura.VENCIDA, StatusAssinatura.AGUARDANDO_PAGAMENTO)
        );

        int atualizadas = 0;
        for (Assinatura assinatura : assinaturas) {
            try {
                AssasSubscriptionResponse subAsaas = assasClient.buscarAssinatura(assinatura.getAssasSubscriptionId());
                if (subAsaas == null) continue;

                boolean atualizado = false;

                // Reconciliar status
                String statusAsaas = subAsaas.getStatus();
                if ("INACTIVE".equals(statusAsaas) || "EXPIRED".equals(statusAsaas)) {
                    if (assinatura.getStatus() == StatusAssinatura.ATIVA) {
                        assinatura.setStatus(StatusAssinatura.VENCIDA);
                        atualizado = true;
                        log.info("Sincronizacao: assinatura {} marcada como VENCIDA (Asaas status: {})",
                                assinatura.getId(), statusAsaas);
                    }
                } else if ("ACTIVE".equals(statusAsaas)) {
                    if (assinatura.getStatus() == StatusAssinatura.VENCIDA
                            || assinatura.getStatus() == StatusAssinatura.AGUARDANDO_PAGAMENTO) {
                        assinatura.setStatus(StatusAssinatura.ATIVA);
                        atualizado = true;
                        log.info("Sincronizacao: assinatura {} reativada (Asaas status: ACTIVE)",
                                assinatura.getId());
                    }
                }

                // Atualizar proximo vencimento do Asaas
                if (subAsaas.getNextDueDate() != null) {
                    try {
                        LocalDate nextDue = LocalDate.parse(subAsaas.getNextDueDate());
                        LocalDateTime nextDueTime = nextDue.atStartOfDay();
                        if (assinatura.getDtProximoVencimento() == null ||
                                !assinatura.getDtProximoVencimento().toLocalDate().equals(nextDue)) {
                            assinatura.setDtProximoVencimento(nextDueTime);
                            atualizado = true;
                        }
                    } catch (Exception e) {
                        log.warn("Erro ao parsear nextDueDate do Asaas: {}", subAsaas.getNextDueDate());
                    }
                }

                if (atualizado) {
                    assinaturaRepository.save(assinatura);
                    atualizadas++;
                }
            } catch (AssasApiException e) {
                log.warn("Erro ao sincronizar assinatura ID {}: {}", assinatura.getId(), e.getMessage());
            }
        }

        if (atualizadas > 0) {
            log.info("Sincronizacao com Asaas concluida: {} assinaturas atualizadas de {} verificadas",
                    atualizadas, assinaturas.size());
        }
    }

    @Transactional
    public void verificarInadimplentes() {
        if (!assasClient.isConfigurado()) return;

        List<Assinatura> ativas = assinaturaRepository.findByStatus(StatusAssinatura.ATIVA);

        // Filter to only those with Asaas subscriptions (non-free plans)
        List<Assinatura> ativasComAsaas = ativas.stream()
                .filter(a -> !a.isPlanoGratuito() && a.getAssasSubscriptionId() != null)
                .collect(Collectors.toList());

        if (ativasComAsaas.isEmpty()) return;

        // Batch approach: try to use buscarPagamentosPorStatus if available
        try {
            AssasPaymentListResponse overduePayments = assasClient.buscarPagamentosPorStatus("OVERDUE");
            if (overduePayments != null && overduePayments.getData() != null) {
                // Build a set of subscription IDs with overdue payments
                Set<String> subscriptionsComOverdue = overduePayments.getData().stream()
                        .filter(p -> p.getSubscription() != null)
                        .map(AssasPaymentResponse::getSubscription)
                        .collect(Collectors.toSet());

                for (Assinatura assinatura : ativasComAsaas) {
                    if (subscriptionsComOverdue.contains(assinatura.getAssasSubscriptionId())) {
                        assinatura.setStatus(StatusAssinatura.VENCIDA);
                        assinaturaRepository.save(assinatura);
                        log.info("Inadimplente detectado na verificacao batch - org: {}",
                                assinatura.getOrganizacao().getId());
                    }
                }
                return;
            }
        } catch (Exception e) {
            log.warn("Erro na verificacao batch de inadimplentes, usando fallback individual: {}", e.getMessage());
        }

        // Fallback: individual check (N+1 but works as backup)
        for (Assinatura assinatura : ativasComAsaas) {
            try {
                AssasPaymentListResponse payments = assasClient.buscarPagamentosAssinatura(assinatura.getAssasSubscriptionId());
                if (payments != null && payments.getData() != null) {
                    boolean temOverdue = payments.getData().stream()
                            .anyMatch(p -> "OVERDUE".equals(p.getStatus()));
                    if (temOverdue && assinatura.getStatus() == StatusAssinatura.ATIVA) {
                        assinatura.setStatus(StatusAssinatura.VENCIDA);
                        assinaturaRepository.save(assinatura);
                        log.info("Inadimplente detectado na verificacao - org: {}", assinatura.getOrganizacao().getId());
                    }
                }
            } catch (AssasApiException e) {
                log.warn("Erro ao verificar inadimplencia para assinatura ID {}: {}", assinatura.getId(), e.getMessage());
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
                .formaPagamento(assinatura.getFormaPagamento() != null ? assinatura.getFormaPagamento().name() : null)
                .dtInicioTrial(assinatura.getDtInicioTrial())
                .dtFimTrial(assinatura.getDtFimTrial())
                .dtInicio(assinatura.getDtInicio())
                .dtProximoVencimento(assinatura.getDtProximoVencimento())
                .dtCancelamento(assinatura.getDtCancelamento())
                .assasCustomerId(assinatura.getAssasCustomerId())
                .assasSubscriptionId(assinatura.getAssasSubscriptionId())
                .cupomCodigo(assinatura.getCupomCodigo())
                .valorDesconto(assinatura.getValorDesconto())
                .creditoProRata(assinatura.getCreditoProRata())
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
                .cupomCodigo(cobranca.getCupomCodigo())
                .valorOriginal(cobranca.getValorOriginal())
                .valorDescontoAplicado(cobranca.getValorDescontoAplicado())
                .dtCriacao(cobranca.getDtCriacao())
                .build();
    }
}
