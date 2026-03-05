package org.exemplo.bellory.service.assinatura;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
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
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.PlanoBelloryRepository;
import org.exemplo.bellory.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
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
                             PlanoBelloryRepository planoBelloryRepository,
                             OrganizacaoRepository organizacaoRepository,
                             CupomDescontoService cupomDescontoService,
                             AssasClient assasClient,
                             EmailService emailService) {
        this.assinaturaRepository = assinaturaRepository;
        this.cobrancaPlataformaRepository = cobrancaPlataformaRepository;
        this.pagamentoPlataformaRepository = pagamentoPlataformaRepository;
        this.planoBelloryRepository = planoBelloryRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.cupomDescontoService = cupomDescontoService;
        this.assasClient = assasClient;
        this.emailService = emailService;
    }

    // ==================== TRIAL ====================

    @Transactional
    public Assinatura criarAssinaturaTrial(Organizacao organizacao, PlanoBellory plano) {
        log.info("Criando assinatura para organizacao: {}", organizacao.getId());

        LocalDateTime agora = LocalDateTime.now();

        // TODO: TESTE - Trial comentado. Criando assinatura ATIVA direto no Assas para validacao.
        // Para restaurar trial, descomentar bloco original e remover bloco de teste abaixo.

        // --- BLOCO ORIGINAL (TRIAL) ---
        // Assinatura assinatura = Assinatura.builder()
        //         .organizacao(organizacao)
        //         .planoBellory(plano)
        //         .status(StatusAssinatura.TRIAL)
        //         .cicloCobranca(CicloCobranca.MENSAL)
        //         .dtInicioTrial(agora)
        //         .dtFimTrial(agora.plusDays(diasTrial))
        //         .valorMensal(plano.getPrecoMensal())
        //         .valorAnual(plano.getPrecoAnual())
        //         .build();
        // return assinaturaRepository.save(assinatura);

        // --- BLOCO DE TESTE (SEM TRIAL - CRIA DIRETO NO ASSAS) ---
        Assinatura assinatura = Assinatura.builder()
                .organizacao(organizacao)
                .planoBellory(plano)
                .status(StatusAssinatura.ATIVA)
                .cicloCobranca(CicloCobranca.MENSAL)
                .formaPagamento(FormaPagamentoPlataforma.PIX)
                .dtInicio(agora)
                .dtProximoVencimento(agora.plusMonths(1))
                .valorMensal(plano.getPrecoMensal())
                .valorAnual(plano.getPrecoAnual())
                .build();

        // Criar cliente no Assas
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

            // Criar assinatura no Assas
            AssasSubscriptionResponse sub = assasClient.criarAssinatura(
                    AssasSubscriptionRequest.builder()
                            .customer(customer.getId())
                            .billingType("PIX")
                            .value(plano.getPrecoMensal())
                            .cycle("MONTHLY")
                            .nextDueDate(LocalDate.now().plusDays(1).toString())
                            .description("Assinatura Bellory - Plano " + plano.getNome())
                            .build()
            );
            if (sub != null) {
                assinatura.setAssasSubscriptionId(sub.getId());
            }
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
                // Cancelada mas ainda pode usar ate o fim do periodo
                if (assinatura.getDtProximoVencimento() != null
                        && LocalDateTime.now().isBefore(assinatura.getDtProximoVencimento())) {
                    builder.bloqueado(false);
                    builder.mensagem("Sua assinatura foi cancelada. Voce pode usar ate " +
                            assinatura.getDtProximoVencimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                            ". Apos isso, sera necessario assinar novamente.");
                } else {
                    builder.bloqueado(true);
                    builder.mensagem("Sua assinatura foi cancelada. Escolha um plano para reativar.");
                }
            }
            case SUSPENSA -> {
                builder.bloqueado(true);
                builder.mensagem("Sua assinatura esta suspensa. Entre em contato com o suporte.");
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

        // Determinar valor para Assas (valor cheio se cupom PRIMEIRA_COBRANCA)
        BigDecimal valorAssas = valorOriginal;
        if (cupomResult != null && cupomResult.isValido()) {
            if (cupomResult.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.RECORRENTE) {
                // Cupom recorrente: Assas cobra o valor com desconto permanentemente
                valorAssas = valor;
            } else {
                // PRIMEIRA_COBRANCA: Assas cobra com desconto, apos pagamento atualiza para valor cheio
                valorAssas = valor;
            }
        }

        // Criar cliente no Assas
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

        // Criar assinatura no Assas
        if (assinatura.getAssasCustomerId() != null) {
            // Se ja existe assinatura no Assas (reativacao), cancela a antiga
            if (assinatura.getAssasSubscriptionId() != null) {
                assasClient.cancelarAssinatura(assinatura.getAssasSubscriptionId());
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

        // Atualizar assinatura
        assinatura.setPlanoBellory(plano);
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setCicloCobranca(ciclo);
        assinatura.setFormaPagamento(forma);
        assinatura.setDtInicio(LocalDateTime.now());
        assinatura.setDtCancelamento(null); // Limpar cancelamento anterior
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

        // Setar campos de cupom na cobranca
        if (cupomResult != null && cupomResult.isValido()) {
            cobranca.setCupom(cupomResult.getCupom());
            cobranca.setValorOriginal(valorOriginal);
            cobranca.setValorDescontoAplicado(cupomResult.getValorDesconto());
            cobranca.setCupomCodigo(cupomResult.getCupom().getCodigo());
        }

        cobrancaPlataformaRepository.save(cobranca);

        // Registrar utilizacao do cupom
        if (cupomResult != null && cupomResult.isValido()) {
            cupomDescontoService.registrarUtilizacao(
                    cupomResult.getCupom(),
                    organizacaoId,
                    assinatura.getId(),
                    cobranca.getId(),
                    valorOriginal,
                    cupomResult.getValorDesconto(),
                    valor,
                    dto.getPlanoCodigo(),
                    dto.getCicloCobranca()
            );
        }

        return toAssinaturaResponseDTO(assinatura);
    }

    // ==================== UPGRADE / DOWNGRADE (9.5) ====================

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

        // Calcular pro-rata do plano atual (credito pelos dias restantes)
        BigDecimal creditoProRata = BigDecimal.ZERO;
        if (assinatura.getDtProximoVencimento() != null && assinatura.getDtInicio() != null) {
            long diasTotais;
            BigDecimal valorAtual;
            if (assinatura.getCicloCobranca() == CicloCobranca.ANUAL) {
                diasTotais = 365;
                valorAtual = assinatura.getValorAnual();
            } else {
                diasTotais = 30;
                valorAtual = assinatura.getValorMensal();
            }

            long diasRestantes = ChronoUnit.DAYS.between(LocalDateTime.now(), assinatura.getDtProximoVencimento());
            if (diasRestantes > 0 && valorAtual != null && valorAtual.compareTo(BigDecimal.ZERO) > 0) {
                creditoProRata = valorAtual
                        .multiply(BigDecimal.valueOf(diasRestantes))
                        .divide(BigDecimal.valueOf(diasTotais), 2, java.math.RoundingMode.HALF_UP);
            }
        }

        // Valor final da primeira cobranca do novo plano = novo valor - credito pro-rata
        BigDecimal valorPrimeiraCobranca = novoValor.subtract(creditoProRata);
        if (valorPrimeiraCobranca.compareTo(BigDecimal.ZERO) < 0) {
            valorPrimeiraCobranca = BigDecimal.ZERO;
        }

        // Validar e aplicar cupom (se informado)
        CupomValidacaoResult cupomResult = null;
        if (dto.getCodigoCupom() != null && !dto.getCodigoCupom().isBlank()) {
            cupomResult = cupomDescontoService.validarCupom(
                    dto.getCodigoCupom(), assinatura.getOrganizacao(), dto.getPlanoCodigo(), dto.getCicloCobranca(), valorPrimeiraCobranca);
            if (!cupomResult.isValido()) {
                throw new IllegalArgumentException("Cupom invalido: " + cupomResult.getMensagem());
            }
            valorPrimeiraCobranca = cupomResult.getValorComDesconto();
        }

        // Cancelar assinatura antiga no Assas
        if (assinatura.getAssasSubscriptionId() != null) {
            assasClient.cancelarAssinatura(assinatura.getAssasSubscriptionId());
        }

        // Criar nova assinatura no Assas
        if (assinatura.getAssasCustomerId() != null) {
            BigDecimal valorAssas = novoValor;
            if (cupomResult != null && cupomResult.isValido()
                    && cupomResult.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.RECORRENTE) {
                valorAssas = cupomDescontoService.calcularDesconto(cupomResult.getCupom(), novoValor);
                valorAssas = novoValor.subtract(valorAssas);
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
        }

        // Atualizar assinatura
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

        // Gerar cobranca pro-rata
        CobrancaPlataforma cobranca = CobrancaPlataforma.builder()
                .assinatura(assinatura)
                .organizacao(assinatura.getOrganizacao())
                .valor(valorPrimeiraCobranca)
                .dtVencimento(LocalDate.now().plusDays(1))
                .status(StatusCobrancaPlataforma.PENDENTE)
                .formaPagamento(forma)
                .referenciaMes(LocalDate.now().getMonthValue())
                .referenciaAno(LocalDate.now().getYear())
                .build();

        if (cupomResult != null && cupomResult.isValido()) {
            cobranca.setCupom(cupomResult.getCupom());
            cobranca.setValorOriginal(novoValor);
            cobranca.setValorDescontoAplicado(cupomResult.getValorDesconto());
            cobranca.setCupomCodigo(cupomResult.getCupom().getCodigo());
        }

        cobrancaPlataformaRepository.save(cobranca);

        log.info("Plano trocado de {} para {} para organizacao ID: {} - credito pro-rata: {} - valor cobranca: {}",
                planoAtual.getCodigo(), novoPlano.getCodigo(), organizacaoId, creditoProRata, valorPrimeiraCobranca);

        return toAssinaturaResponseDTO(assinatura);
    }

    // ==================== CANCELAMENTO (9.6) ====================

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

        // Cancelar renovacao no Assas (bloqueia proximas cobrancas)
        if (assinatura.getAssasSubscriptionId() != null) {
            assasClient.cancelarAssinatura(assinatura.getAssasSubscriptionId());
        }

        // Marcar como cancelada mas manter acesso ate o fim do periodo
        assinatura.setStatus(StatusAssinatura.CANCELADA);
        assinatura.setDtCancelamento(LocalDateTime.now());
        // dtProximoVencimento mantem o valor atual - o cliente usa ate essa data
        assinaturaRepository.save(assinatura);

        // Cancelar cobrancas pendentes futuras
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

        // Criar nova assinatura no Assas
        if (assinatura.getAssasCustomerId() != null) {
            BigDecimal valorAssas = valor;
            if (cupomResult != null && cupomResult.isValido()
                    && cupomResult.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.PRIMEIRA_COBRANCA) {
                valorAssas = valor; // primeira cobranca com desconto, depois atualiza
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

        // Atualizar assinatura - data de inicio = agora, proximo vencimento = +30 dias ou +1 ano
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

        // Gerar cobranca imediata (D+1)
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

        if (cupomResult != null && cupomResult.isValido()) {
            cobranca.setCupom(cupomResult.getCupom());
            cobranca.setValorOriginal(valorOriginal);
            cobranca.setValorDescontoAplicado(cupomResult.getValorDesconto());
            cobranca.setCupomCodigo(cupomResult.getCupom().getCodigo());
        }

        cobrancaPlataformaRepository.save(cobranca);

        if (cupomResult != null && cupomResult.isValido()) {
            cupomDescontoService.registrarUtilizacao(
                    cupomResult.getCupom(),
                    organizacaoId,
                    assinatura.getId(),
                    cobranca.getId(),
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

    // ==================== WEBHOOK (9.4 + 9.7) ====================

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

                    // Garantir que assinatura esta ativa e renovar vencimento
                    Assinatura assinatura = cobranca.getAssinatura();
                    if (assinatura.getStatus() == StatusAssinatura.VENCIDA) {
                        assinatura.setStatus(StatusAssinatura.ATIVA);
                    }

                    // Renovar proximo vencimento baseado no ciclo
                    if (assinatura.getStatus() == StatusAssinatura.ATIVA) {
                        if (assinatura.getCicloCobranca() == CicloCobranca.ANUAL) {
                            assinatura.setDtProximoVencimento(LocalDateTime.now().plusYears(1));
                        } else {
                            assinatura.setDtProximoVencimento(LocalDateTime.now().plusMonths(1));
                        }
                    }

                    assinaturaRepository.save(assinatura);

                    // 9.4: Se cupom era PRIMEIRA_COBRANCA, atualizar assinatura no Assas para valor cheio
                    if (assinatura.getCupom() != null
                            && assinatura.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.PRIMEIRA_COBRANCA
                            && assinatura.getAssasSubscriptionId() != null) {
                        BigDecimal valorCheio = assinatura.getCicloCobranca() == CicloCobranca.ANUAL
                                ? assinatura.getValorAnual() : assinatura.getValorMensal();
                        assasClient.atualizarAssinatura(
                                assinatura.getAssasSubscriptionId(),
                                AssasSubscriptionRequest.builder()
                                        .value(valorCheio)
                                        .build()
                        );
                        // Limpar cupom da assinatura pois ja foi usado
                        assinatura.setCupom(null);
                        assinatura.setValorDesconto(null);
                        assinatura.setCupomCodigo(null);
                        assinaturaRepository.save(assinatura);
                        log.info("Cupom PRIMEIRA_COBRANCA removido - assinatura Assas atualizada para valor cheio: {} - org: {}",
                                valorCheio, assinatura.getOrganizacao().getId());
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

    /**
     * 9.1: Trial expirado -> migra para plano gratuito automaticamente
     */
    @Transactional
    public void expirarTrials() {
        List<Assinatura> trialsExpirados = assinaturaRepository.findTrialsExpirados(LocalDateTime.now());

        PlanoBellory planoGratuito = planoBelloryRepository.findByCodigo(PLANO_GRATUITO_CODIGO)
                .orElse(null);

        for (Assinatura assinatura : trialsExpirados) {
            if (planoGratuito != null) {
                // Migra para plano gratuito com os limites do plano gratuito
                assinatura.setPlanoBellory(planoGratuito);
                assinatura.setStatus(StatusAssinatura.ATIVA);
                assinatura.setDtInicio(LocalDateTime.now());
                assinatura.setValorMensal(BigDecimal.ZERO);
                assinatura.setValorAnual(BigDecimal.ZERO);
                assinaturaRepository.save(assinatura);
                log.info("Trial expirado - migrado para plano gratuito - organizacao ID: {}",
                        assinatura.getOrganizacao().getId());
            } else {
                // Fallback: se nao existe plano gratuito, bloqueia
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
            // Nao notifica plano gratuito
            if (assinatura.isPlanoGratuito()) {
                continue;
            }

            // Nao envia se ja foi notificado
            if (assinatura.getDtTrialNotificado() != null) {
                continue;
            }

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

    /**
     * Gera cobrancas mensais para assinaturas MENSAL ativas.
     * Aplica cupom RECORRENTE se vigente.
     */
    @Transactional
    public void gerarCobrancasMensais() {
        List<Assinatura> ativas = assinaturaRepository.findMensaisAtivas();
        int mesAtual = LocalDate.now().getMonthValue();
        int anoAtual = LocalDate.now().getYear();

        for (Assinatura assinatura : ativas) {
            boolean jaExiste = cobrancaPlataformaRepository
                    .existsByAssinaturaIdAndReferenciaMesAndReferenciaAno(assinatura.getId(), mesAtual, anoAtual);

            if (!jaExiste) {
                BigDecimal valorOriginal = assinatura.getValorMensal();

                if (valorOriginal != null && valorOriginal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal valor = valorOriginal;
                    BigDecimal valorDescontoAplicado = null;
                    CupomDesconto cupomAplicado = null;

                    // Aplicar cupom recorrente se existir e estiver vigente
                    if (assinatura.getCupom() != null
                            && assinatura.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.RECORRENTE
                            && assinatura.getCupom().isVigente()) {
                        cupomAplicado = assinatura.getCupom();
                        valorDescontoAplicado = cupomDescontoService.calcularDesconto(cupomAplicado, valorOriginal);
                        valor = valorOriginal.subtract(valorDescontoAplicado);
                        if (valor.compareTo(BigDecimal.ZERO) < 0) {
                            valor = BigDecimal.ZERO;
                        }
                    }

                    CobrancaPlataforma cobranca = CobrancaPlataforma.builder()
                            .assinatura(assinatura)
                            .organizacao(assinatura.getOrganizacao())
                            .valor(valor)
                            .dtVencimento(LocalDate.now().withDayOfMonth(10))
                            .status(StatusCobrancaPlataforma.PENDENTE)
                            .referenciaMes(mesAtual)
                            .referenciaAno(anoAtual)
                            .build();

                    if (cupomAplicado != null) {
                        cobranca.setValorOriginal(valorOriginal);
                        cobranca.setValorDescontoAplicado(valorDescontoAplicado);
                        cobranca.setCupom(cupomAplicado);
                        cobranca.setCupomCodigo(cupomAplicado.getCodigo());
                    }

                    cobrancaPlataformaRepository.save(cobranca);
                    log.info("Cobranca mensal gerada para assinatura ID: {} - valor: {}{}",
                            assinatura.getId(), valor,
                            cupomAplicado != null ? " (cupom " + cupomAplicado.getCodigo() + " aplicado)" : "");
                }
            }
        }
    }

    /**
     * 9.2 + 9.3: Gera cobrancas para renovacao de assinaturas anuais.
     * Gera cobranca 30 dias antes do vencimento.
     * Aplica cupom RECORRENTE se vigente.
     */
    @Transactional
    public void gerarCobrancasAnuais() {
        // Busca assinaturas anuais que vencem nos proximos 30 dias
        LocalDateTime limite = LocalDateTime.now().plusDays(30);
        List<Assinatura> anuaisParaRenovar = assinaturaRepository.findAnuaisParaRenovacao(limite);
        int mesAtual = LocalDate.now().getMonthValue();
        int anoAtual = LocalDate.now().getYear();

        for (Assinatura assinatura : anuaisParaRenovar) {
            boolean jaExiste = cobrancaPlataformaRepository
                    .existsByAssinaturaIdAndReferenciaMesAndReferenciaAno(assinatura.getId(), mesAtual, anoAtual);

            if (!jaExiste) {
                BigDecimal valorOriginal = assinatura.getValorAnual();

                if (valorOriginal != null && valorOriginal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal valor = valorOriginal;
                    BigDecimal valorDescontoAplicado = null;
                    CupomDesconto cupomAplicado = null;

                    // 9.3: Aplicar cupom recorrente em plano anual se vigente
                    if (assinatura.getCupom() != null
                            && assinatura.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.RECORRENTE
                            && assinatura.getCupom().isVigente()) {
                        cupomAplicado = assinatura.getCupom();
                        valorDescontoAplicado = cupomDescontoService.calcularDesconto(cupomAplicado, valorOriginal);
                        valor = valorOriginal.subtract(valorDescontoAplicado);
                        if (valor.compareTo(BigDecimal.ZERO) < 0) {
                            valor = BigDecimal.ZERO;
                        }
                    }

                    // Vencimento = data do proximo vencimento da assinatura
                    LocalDate dtVencimento = assinatura.getDtProximoVencimento().toLocalDate();

                    CobrancaPlataforma cobranca = CobrancaPlataforma.builder()
                            .assinatura(assinatura)
                            .organizacao(assinatura.getOrganizacao())
                            .valor(valor)
                            .dtVencimento(dtVencimento)
                            .status(StatusCobrancaPlataforma.PENDENTE)
                            .formaPagamento(assinatura.getFormaPagamento())
                            .referenciaMes(mesAtual)
                            .referenciaAno(anoAtual)
                            .build();

                    if (cupomAplicado != null) {
                        cobranca.setValorOriginal(valorOriginal);
                        cobranca.setValorDescontoAplicado(valorDescontoAplicado);
                        cobranca.setCupom(cupomAplicado);
                        cobranca.setCupomCodigo(cupomAplicado.getCodigo());
                    }

                    cobrancaPlataformaRepository.save(cobranca);
                    log.info("Cobranca anual de renovacao gerada para assinatura ID: {} - valor: {} - vencimento: {}{}",
                            assinatura.getId(), valor, dtVencimento,
                            cupomAplicado != null ? " (cupom " + cupomAplicado.getCodigo() + " aplicado)" : "");
                }
            }
        }
    }

    /**
     * Verifica assinaturas canceladas cujo periodo de acesso expirou
     * e bloqueia efetivamente o acesso.
     */
    @Transactional
    public void bloquearCancelamentoExpirado() {
        List<Assinatura> canceladas = assinaturaRepository.findByStatus(StatusAssinatura.CANCELADA);
        PlanoBellory planoGratuito = planoBelloryRepository.findByCodigo(PLANO_GRATUITO_CODIGO)
                .orElse(null);

        for (Assinatura assinatura : canceladas) {
            if (assinatura.getDtProximoVencimento() != null
                    && LocalDateTime.now().isAfter(assinatura.getDtProximoVencimento())) {
                if (planoGratuito != null) {
                    // Migra para plano gratuito
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
