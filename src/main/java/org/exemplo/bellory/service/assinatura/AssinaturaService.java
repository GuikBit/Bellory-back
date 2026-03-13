package org.exemplo.bellory.service.assinatura;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.config.CacheConfig;
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
import org.exemplo.bellory.model.repository.assinatura.PagamentoPlataformaRepository;
import org.exemplo.bellory.model.repository.assinatura.WebhookLogRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.PlanoBelloryRepository;
import org.exemplo.bellory.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
                             AssasClient assasClient,
                             EmailService emailService) {
        this.assinaturaRepository = assinaturaRepository;
        this.cobrancaPlataformaRepository = cobrancaPlataformaRepository;
        this.pagamentoPlataformaRepository = pagamentoPlataformaRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.planoBelloryRepository = planoBelloryRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.cupomDescontoService = cupomDescontoService;
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
                .valorMensal(plano.getPrecoMensal())
                .valorAnual(plano.getPrecoAnual())
                .build();

        // Criar cliente no Asaas antecipadamente (para quando escolher plano ja ter o ID)
        try {
            AssasCustomerResponse customer = assasClient.criarCliente(
                    AssasCustomerRequest.builder()
                            .name(organizacao.getNomeFantasia())
                            .cpfCnpj(organizacao.getCnpj())
                            .email(organizacao.getEmailPrincipal())
                            .phone(organizacao.getTelefone1())
                            .build()
            );
            if (customer != null) {
                assinatura.setAssasCustomerId(customer.getId());
            }
        } catch (AssasApiException e) {
            log.warn("Nao foi possivel criar cliente no Asaas durante trial. Sera criado ao escolher plano. Erro: {}", e.getMessage());
        }

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

            // Validar que dados do cartao foram informados quando necessario
            if ("CREDIT_CARD".equals(billingType)) {
                if ((dto.getCreditCard() == null || dto.getCreditCard().getNumber() == null)
                        && (dto.getCreditCardToken() == null || dto.getCreditCardToken().isBlank())) {
                    throw new IllegalArgumentException(
                            "Dados do cartao de credito ou token sao obrigatorios para pagamento com cartao");
                }
            }

            AssasSubscriptionRequest assasRequest = buildAssasSubscriptionRequest(
                    assinatura.getAssasCustomerId(), billingType, valorAssas,
                    cycle, "Assinatura Bellory - Plano " + plano.getNome(),
                    dto, assinatura.getOrganizacao());

            AssasSubscriptionResponse sub = assasClient.criarAssinatura(assasRequest);
            if (sub != null) {
                assinatura.setAssasSubscriptionId(sub.getId());
            }
        }

        // Atualizar assinatura local
        assinatura.setPlanoBellory(plano);
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setCicloCobranca(ciclo);
        assinatura.setFormaPagamento(forma);
        assinatura.setDtInicio(LocalDateTime.now());
        assinatura.setDtCancelamento(null);
        assinatura.setValorMensal(plano.getPrecoMensal());
        assinatura.setValorAnual(plano.getPrecoAnual());

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

        BigDecimal novoValor = novoCiclo == CicloCobranca.ANUAL ? novoPlano.getPrecoAnual() : novoPlano.getPrecoMensal();
        BigDecimal valorAssas = novoValor;

        // Validar e aplicar cupom
        CupomValidacaoResult cupomResult = null;
        if (dto.getCodigoCupom() != null && !dto.getCodigoCupom().isBlank()) {
            cupomResult = cupomDescontoService.validarCupom(
                    dto.getCodigoCupom(), assinatura.getOrganizacao(), dto.getPlanoCodigo(), dto.getCicloCobranca(), novoValor);
            if (!cupomResult.isValido()) {
                throw new IllegalArgumentException("Cupom invalido: " + cupomResult.getMensagem());
            }
            valorAssas = cupomResult.getValorComDesconto();
        }

        // Cancelar assinatura antiga no Asaas
        if (assinatura.getAssasSubscriptionId() != null) {
            try {
                assasClient.cancelarAssinatura(assinatura.getAssasSubscriptionId());
            } catch (AssasApiException e) {
                log.error("Falha ao cancelar assinatura antiga no Asaas [{}]: {}", assinatura.getAssasSubscriptionId(), e.getMessage());
                throw new IllegalStateException(
                        "Nao foi possivel cancelar a assinatura atual no gateway. Tente novamente em alguns minutos.", e);
            }
        }

        // Criar nova assinatura no Asaas (mesmo padrao do escolherPlano)
        if (assinatura.getAssasCustomerId() != null) {
            String billingType = mapFormaPagamento(forma);
            String cycle = novoCiclo == CicloCobranca.ANUAL ? "YEARLY" : "MONTHLY";

            // Validar que dados do cartao foram informados quando necessario
            if ("CREDIT_CARD".equals(billingType)) {
                if ((dto.getCreditCard() == null || dto.getCreditCard().getNumber() == null)
                        && (dto.getCreditCardToken() == null || dto.getCreditCardToken().isBlank())) {
                    throw new IllegalArgumentException(
                            "Dados do cartao de credito ou token sao obrigatorios para pagamento com cartao");
                }
            }

            // Valor recorrente para o Asaas (com desconto se cupom RECORRENTE)
            BigDecimal valorRecorrente = novoValor;
            if (cupomResult != null && cupomResult.isValido()
                    && cupomResult.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.RECORRENTE) {
                valorRecorrente = novoValor.subtract(cupomDescontoService.calcularDesconto(cupomResult.getCupom(), novoValor));
            }

            AssasSubscriptionRequest assasRequest = buildAssasSubscriptionRequest(
                    assinatura.getAssasCustomerId(), billingType, valorRecorrente,
                    cycle, "Assinatura Bellory - Plano " + novoPlano.getNome(),
                    dto, assinatura.getOrganizacao());

            AssasSubscriptionResponse sub = assasClient.criarAssinatura(assasRequest);
            if (sub != null) {
                assinatura.setAssasSubscriptionId(sub.getId());
            }
        }

        // Atualizar assinatura local
        assinatura.setPlanoBellory(novoPlano);
        assinatura.setCicloCobranca(novoCiclo);
        assinatura.setFormaPagamento(forma);
        assinatura.setDtInicio(LocalDateTime.now());
        assinatura.setValorMensal(novoPlano.getPrecoMensal());
        assinatura.setValorAnual(novoPlano.getPrecoAnual());

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

        // Registrar utilizacao do cupom
        if (cupomResult != null && cupomResult.isValido()) {
            cupomDescontoService.registrarUtilizacao(
                    cupomResult.getCupom(),
                    organizacaoId,
                    assinatura.getId(),
                    null,
                    novoValor,
                    cupomResult.getValorDesconto(),
                    valorAssas,
                    dto.getPlanoCodigo(),
                    dto.getCicloCobranca()
            );
        }

        log.info("Plano trocado de {} para {} para organizacao ID: {}",
                planoAtual.getCodigo(), novoPlano.getCodigo(), organizacaoId);

        return toAssinaturaResponseDTO(assinatura);
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

            // Validar que dados do cartao foram informados quando necessario
            if ("CREDIT_CARD".equals(billingType)) {
                if ((dto.getCreditCard() == null || dto.getCreditCard().getNumber() == null)
                        && (dto.getCreditCardToken() == null || dto.getCreditCardToken().isBlank())) {
                    throw new IllegalArgumentException(
                            "Dados do cartao de credito ou token sao obrigatorios para pagamento com cartao");
                }
            }

            AssasSubscriptionRequest assasRequest = buildAssasSubscriptionRequest(
                    assinatura.getAssasCustomerId(), billingType, valor,
                    cycle, "Assinatura Bellory - Plano " + plano.getNome(),
                    dto, assinatura.getOrganizacao());

            AssasSubscriptionResponse sub = assasClient.criarAssinatura(assasRequest);
            if (sub != null) {
                assinatura.setAssasSubscriptionId(sub.getId());
            }
        }

        assinatura.setPlanoBellory(plano);
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setCicloCobranca(ciclo);
        assinatura.setFormaPagamento(forma);
        assinatura.setDtInicio(LocalDateTime.now());
        assinatura.setDtCancelamento(null);
        assinatura.setValorMensal(plano.getPrecoMensal());
        assinatura.setValorAnual(plano.getPrecoAnual());

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

    /**
     * Constroi o request para o Asaas incluindo dados do cartao de credito quando necessario.
     * Os dados do cartao sao enviados diretamente ao Asaas e NUNCA armazenados localmente.
     */
    private AssasSubscriptionRequest buildAssasSubscriptionRequest(
            String customerId, String billingType, BigDecimal valor,
            String cycle, String description, EscolherPlanoDTO dto,
            Organizacao org) {

        AssasSubscriptionRequest.AssasSubscriptionRequestBuilder builder = AssasSubscriptionRequest.builder()
                .customer(customerId)
                .billingType(billingType)
                .value(valor)
                .cycle(cycle)
                .nextDueDate(LocalDate.now().plusDays(1).toString())
                .description(description);

        // Adicionar dados do cartao apenas para CREDIT_CARD
        if ("CREDIT_CARD".equals(billingType) && dto != null) {
            if (dto.getCreditCardToken() != null && !dto.getCreditCardToken().isBlank()) {
                // Usar token (gerado via Asaas.js no frontend)
                builder.creditCardToken(dto.getCreditCardToken());
            } else if (dto.getCreditCard() != null) {
                // Enviar dados do cartao diretamente ao Asaas (nunca armazenados)
                CreditCardDTO card = dto.getCreditCard();
                builder.creditCard(AssasCreditCardDTO.builder()
                        .holderName(card.getHolderName())
                        .number(card.getNumber())
                        .expiryMonth(card.getExpiryMonth())
                        .expiryYear(card.getExpiryYear())
                        .ccv(card.getCcv())
                        .build());

                // Dados do portador (obrigatorios pelo Asaas para cartao)
                if (org != null) {
                    AssasCreditCardHolderInfoDTO.AssasCreditCardHolderInfoDTOBuilder holderBuilder =
                            AssasCreditCardHolderInfoDTO.builder()
                                    .name(card.getHolderName())
                                    .email(org.getEmailPrincipal())
                                    .cpfCnpj(org.getCnpj())
                                    .phone(org.getTelefone1());

                    if (org.getEnderecoPrincipal() != null) {
                        holderBuilder
                                .postalCode(org.getEnderecoPrincipal().getCep())
                                .addressNumber(org.getEnderecoPrincipal().getNumero());
                    }

                    builder.creditCardHolderInfo(holderBuilder.build());
                }
            }
        }

        return builder.build();
    }

    /**
     * Constroi request de cobranca avulsa para o Asaas incluindo dados de cartao quando necessario.
     */
    private AssasPaymentRequest buildAssasPaymentRequest(
            String customerId, String billingType, BigDecimal valor,
            String dueDate, String description, EscolherPlanoDTO dto,
            Organizacao org) {

        AssasPaymentRequest.AssasPaymentRequestBuilder builder = AssasPaymentRequest.builder()
                .customer(customerId)
                .billingType(billingType)
                .value(valor)
                .dueDate(dueDate)
                .description(description);

        // Adicionar dados do cartao apenas para CREDIT_CARD
        if ("CREDIT_CARD".equals(billingType) && dto != null) {
            if (dto.getCreditCardToken() != null && !dto.getCreditCardToken().isBlank()) {
                builder.creditCardToken(dto.getCreditCardToken());
            } else if (dto.getCreditCard() != null) {
                CreditCardDTO card = dto.getCreditCard();
                builder.creditCard(AssasCreditCardDTO.builder()
                        .holderName(card.getHolderName())
                        .number(card.getNumber())
                        .expiryMonth(card.getExpiryMonth())
                        .expiryYear(card.getExpiryYear())
                        .ccv(card.getCcv())
                        .build());

                if (org != null) {
                    AssasCreditCardHolderInfoDTO.AssasCreditCardHolderInfoDTOBuilder holderBuilder =
                            AssasCreditCardHolderInfoDTO.builder()
                                    .name(card.getHolderName())
                                    .email(org.getEmailPrincipal())
                                    .cpfCnpj(org.getCnpj())
                                    .phone(org.getTelefone1());

                    if (org.getEnderecoPrincipal() != null) {
                        holderBuilder
                                .postalCode(org.getEnderecoPrincipal().getCep())
                                .addressNumber(org.getEnderecoPrincipal().getNumero());
                    }

                    builder.creditCardHolderInfo(holderBuilder.build());
                }
            }
        }

        return builder.build();
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

    // ==================== WEBHOOK ====================

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

    private void confirmarPagamento(AssasWebhookPayload.Payment payment, Assinatura assinatura) {
        if (assinatura == null) {
            log.warn("Assinatura nao encontrada para pagamento confirmado: {}", payment.getId());
            return;
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

        // Garantir que assinatura esta ativa e renovar vencimento
        if (assinatura.getStatus() == StatusAssinatura.VENCIDA) {
            assinatura.setStatus(StatusAssinatura.ATIVA);
        }

        if (assinatura.getStatus() == StatusAssinatura.ATIVA) {
            if (assinatura.getCicloCobranca() == CicloCobranca.ANUAL) {
                assinatura.setDtProximoVencimento(LocalDateTime.now().plusYears(1));
            } else {
                assinatura.setDtProximoVencimento(LocalDateTime.now().plusMonths(1));
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

        log.info("Pagamento confirmado - assinatura org: {}", assinatura.getOrganizacao().getId());
    }

    private void marcarPagamentoAtrasado(AssasWebhookPayload.Payment payment, Assinatura assinatura) {
        // Atualizar cobranca local se existir
        cobrancaPlataformaRepository.findByAssasPaymentId(payment.getId())
                .ifPresent(cobranca -> {
                    cobranca.setStatus(StatusCobrancaPlataforma.VENCIDA);
                    cobrancaPlataformaRepository.save(cobranca);
                });

        if (assinatura != null && assinatura.getStatus() == StatusAssinatura.ATIVA) {
            assinatura.setStatus(StatusAssinatura.VENCIDA);
            assinaturaRepository.save(assinatura);
            log.info("Assinatura marcada como vencida por pagamento atrasado - org: {}", assinatura.getOrganizacao().getId());
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
                assinatura.setValorMensal(BigDecimal.ZERO);
                assinatura.setValorAnual(BigDecimal.ZERO);
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
                    assinatura.setValorMensal(BigDecimal.ZERO);
                    assinatura.setValorAnual(BigDecimal.ZERO);
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
                List.of(StatusAssinatura.ATIVA, StatusAssinatura.VENCIDA)
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
                    if (assinatura.getStatus() == StatusAssinatura.VENCIDA) {
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
        for (Assinatura assinatura : ativas) {
            if (assinatura.isPlanoGratuito() || assinatura.getAssasSubscriptionId() == null) continue;

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
                .valorMensal(assinatura.getValorMensal())
                .valorAnual(assinatura.getValorAnual())
                .assasCustomerId(assinatura.getAssasCustomerId())
                .assasSubscriptionId(assinatura.getAssasSubscriptionId())
                .cupomCodigo(assinatura.getCupomCodigo())
                .valorDesconto(assinatura.getValorDesconto())
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
