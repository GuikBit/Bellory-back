package org.exemplo.bellory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.client.payment.dto.CachedCustomerStatus;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.assinatura.AssinaturaStatusDTO;
import org.exemplo.bellory.model.dto.home.*;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.aviso.AvisoDispensado;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.enums.TipoCategoria;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.organizacao.RedesSociais;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.assinatura.AssinaturaRepository;
import org.exemplo.bellory.model.repository.aviso.AvisoDispensadoRepository;
import org.exemplo.bellory.model.repository.categoria.CategoriaRepository;
import org.exemplo.bellory.model.repository.funcionario.CargoRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.instance.InstanceRepository;
import org.exemplo.bellory.model.repository.organizacao.HorarioFuncionamentoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.exemplo.bellory.service.assinatura.AssinaturaCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizacaoHomeService {

    private final AgendamentoRepository agendamentoRepository;
    private final CobrancaRepository cobrancaRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ServicoRepository servicoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final HorarioFuncionamentoRepository horarioFuncionamentoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final AssinaturaCacheService assinaturaCacheService;
    private final AvisoDispensadoRepository avisoDispensadoRepository;
    private final CargoRepository cargoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InstanceRepository instanceRepository;

    private static final String CARGO_ADMINISTRADOR = "Administrador";
    private static final String FEATURE_AGENTE_VIRTUAL = "agente_virtual";

    // ==================== 1. RESUMO HOME ====================

    @Transactional(readOnly = true)
    public ResumoHomeDTO getResumoHome() {
        Long orgId = getOrganizacaoId();

        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        LocalDateTime fimHoje = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(LocalTime.MAX);

        // Agendamentos hoje
        Long agendamentosHoje = agendamentoRepository.countByOrganizacaoAndPeriodo(orgId, inicioHoje, fimHoje);

        // Faturamento do mes
        BigDecimal faturamentoMes = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(inicioMes, fimMes, orgId);

        // Clientes ativos
        Long clientesAtivos = clienteRepository.countByOrganizacao_IdAndAtivoTrue(orgId);

        // Taxa de ocupacao (agendamentos concluidos / total do mes)
        Long totalMes = agendamentoRepository.countByOrganizacaoAndPeriodo(orgId, inicioMes, fimMes);
        Long concluidosMes = agendamentoRepository.countVendasByOrganizacaoAndPeriodo(orgId, inicioMes, fimMes);
        Double taxaOcupacao = totalMes > 0 ? (concluidosMes.doubleValue() / totalMes.doubleValue()) * 100 : 0.0;

        // Proximo agendamento
        ResumoHomeDTO.ProximoAgendamentoDTO proximo = getProximoAgendamento(orgId);

        return ResumoHomeDTO.builder()
                .agendamentosHoje(agendamentosHoje)
                .faturamentoMes(faturamentoMes != null ? faturamentoMes : BigDecimal.ZERO)
                .clientesAtivos(clientesAtivos)
                .taxaOcupacao(Math.round(taxaOcupacao * 100.0) / 100.0)
                .proximoAgendamento(proximo)
                .build();
    }

    private ResumoHomeDTO.ProximoAgendamentoDTO getProximoAgendamento(Long orgId) {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime fim7dias = agora.plusDays(7);

        List<Agendamento> proximos = agendamentoRepository
                .findProximosAgendamentosByOrganizacaoId(agora, fim7dias, orgId);

        if (proximos.isEmpty()) return null;

        Agendamento a = proximos.get(0);
        String servicoNome = a.getServicos() != null && !a.getServicos().isEmpty()
                ? a.getServicos().stream().map(s -> s.getNome()).collect(Collectors.joining(", "))
                : null;
        String funcNome = a.getFuncionarios() != null && !a.getFuncionarios().isEmpty()
                ? a.getFuncionarios().get(0).getNomeCompleto()
                : null;

        return ResumoHomeDTO.ProximoAgendamentoDTO.builder()
                .id(a.getId())
                .clienteNome(a.getCliente() != null ? a.getCliente().getNomeCompleto() : null)
                .servicoNome(servicoNome)
                .funcionarioNome(funcNome)
                .dtAgendamento(a.getDtAgendamento())
                .build();
    }

    // ==================== 2. AVISOS ====================

    @Transactional(readOnly = true)
    public List<AvisoDTO> getAvisos() {
        Long orgId = getOrganizacaoId();
        Long userId = TenantContext.getCurrentUserId();

        List<String> dispensados = userId != null
                ? avisoDispensadoRepository.findAvisoIdsByOrganizacaoIdAndUsuarioId(orgId, userId)
                : List.of();

        List<AvisoDTO> avisos = new ArrayList<>();

        // === Assinatura / Plano da organizacao (Payment API) ===
        try {
            AssinaturaStatusDTO status = assinaturaCacheService.getStatusByOrganizacao(orgId);
            if (status != null) {
                String situacao = status.getSituacao();

                if ("PAGAMENTO_ATRASADO".equals(situacao)) {
                    String msg = "Sua fatura está vencida.";
                    if (status.getValorPendente() != null) {
                        msg = String.format("Sua fatura de R$ %.2f está vencida. Regularize para evitar suspensão.", status.getValorPendente());
                    }
                    avisos.add(AvisoDTO.builder()
                            .id("assinatura_pagamento_atrasado")
                            .tipo("PLANO")
                            .severidade("ERROR")
                            .titulo("Pagamento atrasado")
                            .mensagem(msg)
                            .cta("Ver assinatura")
                            .dispensavel(false)
                            .build());
                } else if ("SUSPENSA".equals(situacao)) {
                    avisos.add(AvisoDTO.builder()
                            .id("assinatura_suspensa")
                            .tipo("PLANO")
                            .severidade("ERROR")
                            .titulo("Assinatura suspensa")
                            .mensagem(status.getMensagem() != null ? status.getMensagem() : "Sua assinatura foi suspensa. Regularize o pagamento.")
                            .cta("Ver assinatura")
                            .dispensavel(false)
                            .build());
                } else if ("CANCELADA_SEM_ACESSO".equals(situacao)) {
                    String msg = "Sua assinatura foi cancelada.";
                    if (status.getDtAcessoAte() != null) {
                        msg = String.format("Sua assinatura foi cancelada. Acesso até %s.", status.getDtAcessoAte());
                    }
                    avisos.add(AvisoDTO.builder()
                            .id("assinatura_cancelada")
                            .tipo("PLANO")
                            .severidade("ERROR")
                            .titulo("Assinatura cancelada")
                            .mensagem(msg)
                            .cta("Reativar plano")
                            .dispensavel(false)
                            .build());
                } else if (Boolean.TRUE.equals(status.getTemCobrancaPendente())) {
                    String msg = "Você tem uma cobrança pendente do seu plano.";
                    if (status.getValorPendente() != null && status.getDtVencimentoProximaCobranca() != null) {
                        msg = String.format("Fatura de R$ %.2f com vencimento em %s.", status.getValorPendente(), status.getDtVencimentoProximaCobranca());
                    }
                    avisos.add(AvisoDTO.builder()
                            .id("assinatura_cobranca_pendente")
                            .tipo("PLANO")
                            .severidade("WARNING")
                            .titulo("Fatura pendente do plano")
                            .mensagem(msg)
                            .cta("Ver assinatura")
                            .dispensavel(true)
                            .build());
                }

                // Trial acabando
                if (status.getDiasRestantesTrial() != null && status.getDiasRestantesTrial() <= 7 && status.getDiasRestantesTrial() > 0) {
                    avisos.add(AvisoDTO.builder()
                            .id("trial_expirando")
                            .tipo("PLANO")
                            .severidade("WARNING")
                            .titulo("Trial expirando")
                            .mensagem(String.format("Seu período de teste termina em %d dia(s). Escolha um plano para continuar.", status.getDiasRestantesTrial()))
                            .cta("Ver planos")
                            .dispensavel(true)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel consultar status da assinatura para org={}: {}", orgId, e.getMessage());
        }

        // === Cobrancas de clientes vencidas ===
        List<Cobranca> vencidas = cobrancaRepository.findVencidasByOrganizacao(orgId);
        if (!vencidas.isEmpty()) {
            BigDecimal totalVencido = cobrancaRepository.sumValorVencidoByOrganizacao(orgId);
            avisos.add(AvisoDTO.builder()
                    .id("cobranca_vencida")
                    .tipo("COBRANCA")
                    .severidade("ERROR")
                    .titulo("Cobranças vencidas")
                    .mensagem(String.format("Você tem %d cobrança(s) vencida(s) totalizando R$ %.2f",
                            vencidas.size(), totalVencido))
                    .cta("Ver cobranças")
                    .dispensavel(true)
                    .build());
        }

        // Cobrancas pendentes com valor alto
        BigDecimal pendente = cobrancaRepository.sumValorPendenteByOrganizacao(orgId);
        if (pendente != null && pendente.compareTo(BigDecimal.ZERO) > 0) {
            avisos.add(AvisoDTO.builder()
                    .id("cobranca_pendente")
                    .tipo("COBRANCA")
                    .severidade("WARNING")
                    .titulo("Cobranças pendentes")
                    .mensagem(String.format("R$ %.2f em cobranças pendentes de recebimento", pendente))
                    .cta("Ver financeiro")
                    .dispensavel(true)
                    .build());
        }

        // Limites do plano - funcionarios
        long totalFunc = funcionarioRepository.countByOrganizacao_IdAndIsDeletadoFalse(orgId);
        if (totalFunc >= 5) {
            avisos.add(AvisoDTO.builder()
                    .id("limite_funcionarios")
                    .tipo("LIMITE")
                    .severidade("WARNING")
                    .titulo("Limite de colaboradores")
                    .mensagem(String.format("Você possui %d colaboradores cadastrados. Verifique o limite do seu plano.", totalFunc))
                    .cta("Ver plano")
                    .dispensavel(true)
                    .build());
        }

        // Onboarding incompleto
        ChecklistOnboardingDTO checklist = getChecklistOnboarding();
        if (checklist.getPercentualCompleto() < 100) {
            avisos.add(AvisoDTO.builder()
                    .id("onboarding_incompleto")
                    .tipo("SISTEMA")
                    .severidade("INFO")
                    .titulo("Complete a configuração")
                    .mensagem(String.format("Sua conta está %d%% configurada. Complete o setup para aproveitar ao máximo.", checklist.getPercentualCompleto()))
                    .cta("Ver checklist")
                    .dispensavel(true)
                    .build());
        }

        // Filtra dispensados
        avisos.removeIf(aviso -> dispensados.contains(aviso.getId()));

        return avisos;
    }

    // ==================== 3. DISPENSAR AVISO ====================

    @Transactional
    public void dispensarAviso(String avisoId) {
        Long orgId = getOrganizacaoId();
        Long userId = TenantContext.getCurrentUserId();

        if (userId == null) {
            throw new SecurityException("Usuário não identificado no token");
        }

        // Evita duplicata
        if (avisoDispensadoRepository.findByOrganizacaoIdAndUsuarioIdAndAvisoId(orgId, userId, avisoId).isPresent()) {
            return;
        }

        AvisoDispensado dispensado = AvisoDispensado.builder()
                .organizacaoId(orgId)
                .usuarioId(userId)
                .avisoId(avisoId)
                .build();
        avisoDispensadoRepository.save(dispensado);
    }

    // ==================== 4. CHECKLIST ONBOARDING ====================

    @Transactional(readOnly = true)
    public ChecklistOnboardingDTO getChecklistOnboarding() {
        Long orgId = getOrganizacaoId();

        Organizacao org = organizacaoRepository.findByIdWithDetails(orgId).orElse(null);

        boolean empresaConfigurada = org != null
                && org.getNomeFantasia() != null
                && org.getEmailPrincipal() != null
                && org.getTelefone1() != null;

        boolean enderecoPrincipalCadastrado = org != null && org.getEnderecoPrincipal() != null;

        // OBS: HorarioFuncionamentoService.listar() seedha 7 placeholders inativos quando vazio,
        // então .isEmpty() não funciona. Usa contagem de dias ATIVOS com período cadastrado.
        boolean horariosDefinidos = horarioFuncionamentoRepository.countDiasAtivosComPeriodo(orgId) > 0;

        boolean redesSociaisCadastradas = org != null && hasRedeSocialPreenchida(org.getRedesSociais());

        boolean logoEnviada = org != null && org.getLogoUrl() != null && !org.getLogoUrl().isBlank();

        boolean bannerEnviado = org != null && org.getBannerUrl() != null && !org.getBannerUrl().isBlank();

        // O cargo "Administrador" é criado automaticamente em OrganizacaoService.create — não conta.
        boolean cargosCadastrados = cargoRepository.findAllByOrganizacao_IdAndAtivoTrue(orgId).stream()
                .anyMatch(c -> !CARGO_ADMINISTRADOR.equalsIgnoreCase(c.getNome()));

        boolean categoriasServicoCadastradas = !categoriaRepository
                .findByOrganizacao_IdAndTipoAndAtivoTrue(orgId, TipoCategoria.SERVICO).isEmpty();

        boolean servicosCadastrados = servicoRepository.countByOrganizacao_IdAndIsDeletadoFalse(orgId) > 0;
        boolean colaboradoresCadastrados = funcionarioRepository.countByOrganizacao_IdAndIsDeletadoFalse(orgId) > 0;

        LocalDateTime inicio = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.now().plusYears(1);
        boolean primeiroAgendamento = agendamentoRepository.countByOrganizacaoAndPeriodo(orgId, inicio, fim) > 0;

        boolean planoEscolhido = assinaturaRepository.findByOrganizacaoId(orgId).isPresent();

        boolean agenteVirtualHabilitado = isFeatureHabilitada(orgId, FEATURE_AGENTE_VIRTUAL);
        boolean agenteVirtualConfigurado = instanceRepository.countByOrganizacaoIdAndDeletadoFalse(orgId) > 0;

        List<EtapaOnboardingDTO> etapas = new ArrayList<>();
        int ordem = 1;
        etapas.add(EtapaOnboardingDTO.of("ENDERECO_PRINCIPAL", "Endereço principal", enderecoPrincipalCadastrado, true, ordem++));
        etapas.add(EtapaOnboardingDTO.of("HORARIO_FUNCIONAMENTO", "Horário de funcionamento", horariosDefinidos, true, ordem++));
        etapas.add(EtapaOnboardingDTO.of("REDES_SOCIAIS", "Redes sociais", redesSociaisCadastradas, true, ordem++));
        etapas.add(EtapaOnboardingDTO.of("LOGO", "Logo", logoEnviada, true, ordem++));
        etapas.add(EtapaOnboardingDTO.of("BANNER", "Banner", bannerEnviado, true, ordem++));
        etapas.add(EtapaOnboardingDTO.of("CARGOS", "Cargos de funcionários", cargosCadastrados, true, ordem++));
        etapas.add(EtapaOnboardingDTO.of("CATEGORIAS_SERVICO", "Categorias de serviços", categoriasServicoCadastradas, true, ordem++));
        if (agenteVirtualHabilitado) {
            etapas.add(EtapaOnboardingDTO.of("AGENTE_VIRTUAL_CONFIG", "Configuração do agente virtual", agenteVirtualConfigurado, true, ordem++));
        }

        boolean setupCompleto = etapas.stream()
                .filter(EtapaOnboardingDTO::isObrigatoria)
                .allMatch(EtapaOnboardingDTO::isConcluida);

        long obrigatorias = etapas.stream().filter(EtapaOnboardingDTO::isObrigatoria).count();
        long obrigatoriasConcluidas = etapas.stream()
                .filter(e -> e.isObrigatoria() && e.isConcluida())
                .count();
        int percentual = obrigatorias > 0
                ? (int) Math.round((obrigatoriasConcluidas * 100.0) / obrigatorias)
                : 100;

        return ChecklistOnboardingDTO.builder()
                .empresaConfigurada(empresaConfigurada)
                .horariosDefinidos(horariosDefinidos)
                .servicosCadastrados(servicosCadastrados)
                .colaboradoresCadastrados(colaboradoresCadastrados)
                .primeiroAgendamento(primeiroAgendamento)
                .logoEnviada(logoEnviada)
                .planoEscolhido(planoEscolhido)
                .percentualCompleto(percentual)
                .setupCompleto(setupCompleto)
                .etapas(etapas)
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isSetupCompleto(Long organizacaoId) {
        if (organizacaoId == null) return false;
        Organizacao org = organizacaoRepository.findByIdWithDetails(organizacaoId).orElse(null);
        if (org == null) return false;

        if (org.getEnderecoPrincipal() == null) return false;
        if (horarioFuncionamentoRepository.countDiasAtivosComPeriodo(organizacaoId) == 0) return false;
        if (!hasRedeSocialPreenchida(org.getRedesSociais())) return false;
        if (org.getLogoUrl() == null || org.getLogoUrl().isBlank()) return false;
        if (org.getBannerUrl() == null || org.getBannerUrl().isBlank()) return false;
        boolean temCargoOperacional = cargoRepository.findAllByOrganizacao_IdAndAtivoTrue(organizacaoId).stream()
                .anyMatch(c -> !CARGO_ADMINISTRADOR.equalsIgnoreCase(c.getNome()));
        if (!temCargoOperacional) return false;
        if (categoriaRepository.findByOrganizacao_IdAndTipoAndAtivoTrue(organizacaoId, TipoCategoria.SERVICO).isEmpty()) return false;
        if (isFeatureHabilitada(organizacaoId, FEATURE_AGENTE_VIRTUAL)
                && instanceRepository.countByOrganizacaoIdAndDeletadoFalse(organizacaoId) == 0) {
            return false;
        }
        return true;
    }

    private boolean hasRedeSocialPreenchida(RedesSociais r) {
        if (r == null) return false;
        return isPreenchido(r.getInstagram())
                || isPreenchido(r.getFacebook())
                || isPreenchido(r.getWhatsapp())
                || isPreenchido(r.getLinkedin())
                || isPreenchido(r.getMessenger())
                || isPreenchido(r.getSite())
                || isPreenchido(r.getYoutube());
    }

    private boolean isPreenchido(String s) {
        return s != null && !s.isBlank();
    }

    private boolean isFeatureHabilitada(Long orgId, String key) {
        try {
            CachedCustomerStatus status = assinaturaCacheService.getCachedByOrganizacao(orgId);
            if (status == null || status.getFeatures() == null) return false;
            return status.getFeatures().stream()
                    .anyMatch(f -> key.equals(f.getKey()) && Boolean.TRUE.equals(f.getEnabled()));
        } catch (Exception e) {
            log.warn("Falha ao consultar feature '{}' do plano org={}: {}", key, orgId, e.getMessage());
            return false;
        }
    }

    // ==================== 5. ATIVIDADE RECENTE ====================

    @Transactional(readOnly = true)
    public List<AtividadeRecenteDTO> getAtividadeRecente() {
        Long orgId = getOrganizacaoId();

        LocalDateTime limite = LocalDateTime.now().minusDays(7);
        LocalDateTime agora = LocalDateTime.now();

        List<AtividadeRecenteDTO> atividades = new ArrayList<>();

        // Ultimos agendamentos criados
        List<Agendamento> agendamentos = agendamentoRepository
                .findByOrganizacaoAndPeriodoWithDetails(orgId, limite, agora);

        agendamentos.stream()
                .sorted(Comparator.comparing(Agendamento::getDtCriacao, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .forEach(a -> atividades.add(AtividadeRecenteDTO.builder()
                        .tipo("AGENDAMENTO")
                        .descricao(String.format("Agendamento %s - %s",
                                a.getStatus(),
                                a.getCliente() != null ? a.getCliente().getNomeCompleto() : "Cliente"))
                        .data(a.getDtCriacao() != null ? a.getDtCriacao() : a.getDtAgendamento())
                        .icone("calendar")
                        .build()));

        // Ultimas cobrancas pagas
        LocalDateTime inicioSemana = limite;
        List<Cobranca> cobrancas = cobrancaRepository.findVencidasByOrganizacao(orgId);
        // Incluir pagas recentes tambem
        cobrancas.stream()
                .limit(5)
                .forEach(c -> atividades.add(AtividadeRecenteDTO.builder()
                        .tipo("COBRANCA")
                        .descricao(String.format("Cobrança %s - R$ %.2f",
                                c.getStatusCobranca(),
                                c.getValor()))
                        .data(c.getDtCriacao())
                        .icone("dollar-sign")
                        .build()));

        // Ordena por data desc e limita
        atividades.sort(Comparator.comparing(AtividadeRecenteDTO::getData, Comparator.nullsLast(Comparator.reverseOrder())));

        return atividades.stream().limit(15).collect(Collectors.toList());
    }

    // ==================== UTILS ====================

    private Long getOrganizacaoId() {
        Long orgId = TenantContext.getCurrentOrganizacaoId();
        if (orgId == null) {
            throw new SecurityException("Organização não identificada no token");
        }
        return orgId;
    }
}
