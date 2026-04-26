package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.*;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.AgendamentoQuestionario;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.agendamento.StatusQuestionarioAgendamento;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.entity.notificacao.ConfigNotificacao;
import org.exemplo.bellory.model.entity.notificacao.NotificacaoEnviada;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.entity.organizacao.BloqueioOrganizacao;
import org.exemplo.bellory.model.event.AgendamentoCanceladoEvent;
import org.exemplo.bellory.model.event.AgendamentoConfirmadoEvent;
import org.exemplo.bellory.model.event.AgendamentoCriadoEvent;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.funcionario.DisponibilidadeRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.funcionario.JornadaTrabalhoRepository;
import org.exemplo.bellory.model.repository.notificacao.ConfigNotificacaoRepository;
import org.exemplo.bellory.model.repository.notificacao.NotificacaoEnviadaRepository;
import org.exemplo.bellory.model.repository.organizacao.BloqueioOrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.exemplo.bellory.model.dto.AgendamentoQuestionarioDetalheDTO;
import org.exemplo.bellory.model.dto.questionario.RespostaQuestionarioDTO;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoQuestionarioRepository;
import org.exemplo.bellory.service.anamnese.AnamneseWhatsAppService;
import org.exemplo.bellory.service.plano.LimiteValidatorService;
import org.exemplo.bellory.service.plano.LimiteValidatorService.TipoLimite;
import org.exemplo.bellory.service.questionario.RespostaQuestionarioService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.temporal.TemporalAdjusters;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final JornadaTrabalhoRepository jornadaTrabalhoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ServicoRepository servicoRepository;
    private final ClienteRepository clienteRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final CobrancaRepository cobrancaRepository;
    private final BloqueioOrganizacaoRepository bloqueioOrganizacaoRepository;
    private final NotificacaoEnviadaRepository notificacaoEnviadaRepository;
    private final ConfigNotificacaoRepository configNotificacaoRepository;

    private final TransacaoService transacaoService;
    private final ApplicationEventPublisher eventPublisher;
    private final LimiteValidatorService limiteValidator;
    private final RespostaQuestionarioService respostaQuestionarioService;
    private final AgendamentoQuestionarioRepository agendamentoQuestionarioRepository;
    private final AnamneseWhatsAppService anamneseWhatsAppService;


    //private static final int TOLERANCIA_MINUTOS = 10;

    // Mapa de transições de status permitidas
    private static final Map<Status, List<Status>> STATUS_TRANSITIONS = Map.ofEntries(
            Map.entry(Status.PENDENTE, List.of(Status.AGENDADO, Status.CANCELADO)),
            Map.entry(Status.AGENDADO, List.of(Status.AGUARDANDO_CONFIRMACAO, Status.CONFIRMADO, Status.CONCLUIDO, Status.REAGENDADO, Status.CANCELADO)),
            Map.entry(Status.AGUARDANDO_CONFIRMACAO, List.of(Status.CONFIRMADO, Status.REAGENDADO, Status.CANCELADO, Status.NAO_COMPARECEU)),
            Map.entry(Status.CONFIRMADO, List.of(Status.EM_ESPERA, Status.CONCLUIDO, Status.REAGENDADO, Status.CANCELADO, Status.NAO_COMPARECEU)),
            Map.entry(Status.EM_ESPERA, List.of(Status.EM_ANDAMENTO,Status.CONCLUIDO, Status.CANCELADO)),
            Map.entry(Status.EM_ANDAMENTO, List.of(Status.CONCLUIDO, Status.CANCELADO)),
            Map.entry(Status.REAGENDADO, List.of(Status.AGUARDANDO_CONFIRMACAO, Status.REAGENDADO, Status.CANCELADO)),
            // Estados finais - sem transições
            Map.entry(Status.CONCLUIDO, List.of()),
            Map.entry(Status.CANCELADO, List.of()),
            Map.entry(Status.NAO_COMPARECEU, List.of())
    );

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              DisponibilidadeRepository disponibilidadeRepository,
                              JornadaTrabalhoRepository jornadaTrabalhoRepository,
                              FuncionarioRepository funcionarioRepository,
                              ServicoRepository servicoRepository,
                              ClienteRepository clienteRepository,
                              OrganizacaoRepository organizacaoRepository,
                              CobrancaRepository cobrancaRepository,
                              BloqueioOrganizacaoRepository bloqueioOrganizacaoRepository,
                              NotificacaoEnviadaRepository notificacaoEnviadaRepository,
                              ConfigNotificacaoRepository configNotificacaoRepository,
                              TransacaoService transacaoService,
                              ApplicationEventPublisher eventPublisher,
                              LimiteValidatorService limiteValidator,
                              RespostaQuestionarioService respostaQuestionarioService,
                              AgendamentoQuestionarioRepository agendamentoQuestionarioRepository,
                              AnamneseWhatsAppService anamneseWhatsAppService) {
        this.agendamentoRepository = agendamentoRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.jornadaTrabalhoRepository = jornadaTrabalhoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.servicoRepository = servicoRepository;
        this.clienteRepository = clienteRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.cobrancaRepository = cobrancaRepository;
        this.bloqueioOrganizacaoRepository = bloqueioOrganizacaoRepository;
        this.notificacaoEnviadaRepository = notificacaoEnviadaRepository;
        this.configNotificacaoRepository = configNotificacaoRepository;
        this.transacaoService = transacaoService;
        this.eventPublisher = eventPublisher;
        this.limiteValidator = limiteValidator;
        this.respostaQuestionarioService = respostaQuestionarioService;
        this.agendamentoQuestionarioRepository = agendamentoQuestionarioRepository;
        this.anamneseWhatsAppService = anamneseWhatsAppService;
    }



//    @Transactional
//    public Agendamento criarAgendamento(Agendamento novoAgendamento) {
//        // 1. Validar a Data e Hora do Agendamento
//        if (novoAgendamento.getDtAgendamento().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Não é possível agendar para o passado.");
//        }
//
//        // 2. Calcular a Duração Total dos Serviços
//        int duracaoTotalMinutos = novoAgendamento.getServicos().stream()
//                .mapToInt(Servico::getTempoEstimadoMinutos)
//                .sum();
//
//        // 3. Definir o Horário de Término do Agendamento
//        LocalDateTime dataHoraFimAgendamento = novoAgendamento.getDtAgendamento().plusMinutes(duracaoTotalMinutos);
//
//        // 4. Para cada funcionário envolvido no agendamento:
//        for (Funcionario funcionario : novoAgendamento.getFuncionarios()) {
//            // a. Verificar a Jornada de Trabalho do Funcionário
//            DayOfWeek diaSemanaAgendamento = novoAgendamento.getDtAgendamento().getDayOfWeek();
//            LocalTime horaInicioAgendamento = novoAgendamento.getDtAgendamento().toLocalTime();
//            LocalTime horaFimAgendamento = dataHoraFimAgendamento.toLocalTime();
//
//            // Encontre a jornada de trabalho para o dia específico
//            JornadaTrabalho jornada = jornadaTrabalhoRepository.findByFuncionarioAndDiaSemana(
//                    funcionario,
//                    DiaSemana.valueOf(diaSemanaAgendamento.name()) // Segundo parâmetro
//            ).orElseThrow(() ->
//                    new RuntimeException("Funcionário não tem jornada de trabalho definida para este dia.")
//            );
//
//            // Verifica se o agendamento está dentro da jornada
//            if (horaInicioAgendamento.isBefore(jornada.getHoraInicio()) ||
//                    horaFimAgendamento.isAfter(jornada.getHoraFim())) {
//                throw new IllegalArgumentException("Funcionário " + funcionario.getNomeCompleto() + " não está disponível neste horário de acordo com sua jornada.");
//            }
//
//            // b. Verificar Conflitos de Bloqueio na Agenda do Funcionário
//            List<BloqueioAgenda> bloqueiosConflitantes = disponibilidadeRepository.findByFuncionarioAndInicioBloqueioBetween(
//                    funcionario,
//                    novoAgendamento.getDtAgendamento(),
//                    dataHoraFimAgendamento
//            );
//
//            // Filtra bloqueios que realmente se sobrepõem
//            boolean temConflito = bloqueiosConflitantes.stream().anyMatch(bloqueio ->
//                    (novoAgendamento.getDtAgendamento().isBefore(bloqueio.getFimBloqueio()) &&
//                            dataHoraFimAgendamento.isAfter(bloqueio.getInicioBloqueio()))
//            );
//
//            if (temConflito) {
//                throw new IllegalArgumentException("Funcionário " + funcionario.getNomeCompleto() + " já possui um bloqueio na agenda neste período.");
//            }
//        }
//
//        // 5. Se todas as validações passarem, salve o agendamento
//        Agendamento agendamentoSalvo = agendamentoRepository.save(novoAgendamento);
//
//        // 6. Crie os Bloqueios de Agenda para os funcionários envolvidos
//        for (Funcionario funcionario : novoAgendamento.getFuncionarios()) {
//            BloqueioAgenda bloqueio = new BloqueioAgenda(
//                    funcionario,
//                    novoAgendamento.getDtAgendamento(),
//                    dataHoraFimAgendamento,
//                    "Agendamento de Serviço",
//                    TipoBloqueio.AGENDAMENTO,
//                    agendamentoSalvo
//            );
//            disponibilidadeRepository.save(bloqueio);
//        }
//
//        return agendamentoSalvo;
//    }

    private void validarOrganizacao(Long entityOrganizacaoId) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada no token");
        }

        if (!organizacaoId.equals(entityOrganizacaoId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
        }
    }

    /**
     * Reenvia a mensagem WhatsApp de um questionário específico vinculado ao agendamento.
     * Usado quando o disparo automático falhou (instância offline na hora do agendamento)
     * ou quando o admin quer empurrar de novo. Reseta o AQ para PENDENTE e dispara o
     * fluxo de envio. Como dispararParaAgendamento processa todos os PENDENTE do
     * agendamento, outros questionários não enviados também serão tentados — comportamento
     * desejável: 1 click envia tudo que está pendente.
     */
    @org.springframework.transaction.annotation.Transactional
    public AgendamentoQuestionarioDetalheDTO reenviarQuestionario(Long agendamentoId, Long aqId) {
        AgendamentoQuestionario aq = agendamentoQuestionarioRepository.findById(aqId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Questionário do agendamento (ID " + aqId + ") não encontrado."));

        if (aq.getAgendamento() == null || !aq.getAgendamento().getId().equals(agendamentoId)) {
            throw new IllegalArgumentException(
                    "Questionário " + aqId + " não pertence ao agendamento " + agendamentoId + ".");
        }

        validarOrganizacao(aq.getAgendamento().getOrganizacao().getId());

        if (aq.getStatus() == StatusQuestionarioAgendamento.RESPONDIDO) {
            throw new IllegalStateException(
                    "Este questionário já foi respondido pelo cliente — reenvio não permitido.");
        }

        // Reseta para PENDENTE e limpa marca de envio anterior. O dispararParaAgendamento
        // processa todos os AQs em PENDENTE do agendamento.
        aq.setStatus(StatusQuestionarioAgendamento.PENDENTE);
        aq.setDtEnvio(null);
        agendamentoQuestionarioRepository.save(aq);

        // Chamada SÍNCRONA — queremos retornar o status atualizado pro front saber
        // imediatamente se o envio funcionou (ENVIADO) ou continuou falhando (FALHOU
        // se houve exceção; PENDENTE se a instância está offline).
        anamneseWhatsAppService.dispararParaAgendamento(agendamentoId);

        // Recarrega para pegar o status final (modificado pelo dispararParaAgendamento)
        AgendamentoQuestionario atualizado = agendamentoQuestionarioRepository.findById(aqId)
                .orElseThrow(() -> new IllegalStateException("Questionário desapareceu durante o reenvio."));

        RespostaQuestionarioDTO resposta = null;
        if (atualizado.getRespostaQuestionarioId() != null) {
            try {
                resposta = respostaQuestionarioService.buscarPorId(atualizado.getRespostaQuestionarioId());
            } catch (IllegalArgumentException ignored) {
                // resposta já existia mas foi removida — segue sem ela
            }
        }
        return AgendamentoQuestionarioDetalheDTO.of(atualizado, resposta);
    }

    /**
     * Lista os questionários (anamneses) vinculados a um agendamento, incluindo a
     * resposta completa quando já preenchida. Pensado para a tela de detalhes do
     * agendamento — 1 chamada do front renderiza tudo (status + perguntas + respostas).
     *
     * Para AQs em status RESPONDIDO, faz fetch da RespostaQuestionario completa via
     * RespostaQuestionarioService.buscarPorId (que enriquece com nomes de cliente/colaborador
     * e devolve as perguntas/opções).
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<AgendamentoQuestionarioDetalheDTO> getQuestionariosDoAgendamento(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Agendamento com ID " + agendamentoId + " não encontrado."));

        validarOrganizacao(agendamento.getOrganizacao().getId());

        if (agendamento.getQuestionarios() == null || agendamento.getQuestionarios().isEmpty()) {
            return List.of();
        }

        return agendamento.getQuestionarios().stream()
                .map(aq -> {
                    RespostaQuestionarioDTO resposta = null;
                    if (aq.getRespostaQuestionarioId() != null) {
                        try {
                            resposta = respostaQuestionarioService.buscarPorId(aq.getRespostaQuestionarioId());
                        } catch (IllegalArgumentException e) {
                            // Resposta foi deletada manualmente — segue sem ela em vez de quebrar.
                            resposta = null;
                        }
                    }
                    return AgendamentoQuestionarioDetalheDTO.of(aq, resposta);
                })
                .collect(Collectors.toList());
    }

    /**
     * Coleta os questionários (anamneses) configurados nos serviços do agendamento e
     * vincula-os ao agendamento como AgendamentoQuestionario(status=PENDENTE).
     * Deduplicado: se múltiplos serviços apontarem para o mesmo questionário, o cliente
     * responde apenas uma vez. Serviços sem anamnese são ignorados.
     *
     * Estratégia incremental: NÃO apaga AgendamentoQuestionario já existentes (que podem
     * estar ENVIADO ou RESPONDIDO). Apenas adiciona vínculos para questionários que
     * ainda não estavam ligados ao agendamento.
     */
    private void vincularQuestionariosDosServicos(Agendamento agendamento) {
        if (agendamento.getQuestionarios() == null) {
            agendamento.setQuestionarios(new ArrayList<>());
        }
        if (agendamento.getServicos() == null || agendamento.getServicos().isEmpty()) {
            return;
        }

        Set<Long> jaVinculados = agendamento.getQuestionarios().stream()
                .map(aq -> aq.getQuestionario().getId())
                .collect(Collectors.toSet());

        agendamento.getServicos().stream()
                .map(Servico::getAnamnese)
                .filter(Objects::nonNull)
                .distinct()
                .filter(q -> !jaVinculados.contains(q.getId()))
                .forEach(q -> {
                    AgendamentoQuestionario aq = AgendamentoQuestionario.builder()
                            .agendamento(agendamento)
                            .questionario(q)
                            .status(StatusQuestionarioAgendamento.PENDENTE)
                            .dtCriacao(LocalDateTime.now())
                            .build();
                    agendamento.getQuestionarios().add(aq);
                    jaVinculados.add(q.getId());
                });
    }

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }

        return organizacaoId;
    }

    /**
     * Método central para processar um agendamento completo.
     * Valida, salva, cria bloqueios, gera cobrança, vincula ao financeiro e publica evento de notificação.
     * Usado tanto pelo endpoint interno quanto pelo endpoint público (booking externo).
     */
    @Transactional
    public Agendamento processarAgendamento(Agendamento agendamento) {
        // 1. Validar e salvar agendamento + criar bloqueios de agenda
        Agendamento agendamentoSalvo = criarAgendamento(agendamento);

        // 1.1. Vincular questionários (anamneses) derivados dos serviços do agendamento.
        // O save final logo abaixo persiste a join table app.agendamento_questionario.
        vincularQuestionariosDosServicos(agendamentoSalvo);

        // 2. Criar cobrança através do TransactionService
        Cobranca cobranca = transacaoService.criarCobrancaParaAgendamento(agendamentoSalvo);

        // 3. Estabelecer relacionamento bidirecional
        agendamentoSalvo.adicionarCobranca(cobranca);
        agendamentoRepository.save(agendamentoSalvo);

        // 4. Publicar evento de agendamento criado
        String nomeServicos = agendamentoSalvo.getServicos().stream()
                .map(Servico::getNome).collect(Collectors.joining(", "));
        String nomeProfissional = agendamentoSalvo.getFuncionarios().stream()
                .map(Funcionario::getNomeCompleto).collect(Collectors.joining(", "));
        BigDecimal valorTotal = agendamentoSalvo.getServicos().stream()
                .map(s -> s.getPrecoFinal() != null && s.getPrecoFinal().compareTo(BigDecimal.ZERO) > 0
                        ? s.getPrecoFinal() : s.getPreco())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        eventPublisher.publishEvent(new AgendamentoCriadoEvent(
                this,
                agendamentoSalvo.getId(),
                agendamentoSalvo.getCliente().getId(),
                agendamentoSalvo.getCliente().getNomeCompleto(),
                agendamentoSalvo.getOrganizacao().getId(),
                agendamentoSalvo.getDtAgendamento(),
                nomeServicos,
                nomeProfissional,
                valorTotal
        ));

        return agendamentoSalvo;
    }

    @Transactional
    public AgendamentoDTO createAgendamentoCompleto(AgendamentoCreateDTO dto) {
        // 1. Buscar entidades relacionadas
        Long organizacaoId = getOrganizacaoIdFromContext();

        // Valida limite de agendamentos do mes corrente (key 'agendamento' nos planos)
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
        long totalMesAtual = agendamentoRepository.countByOrganizacaoAndPeriodo(organizacaoId, inicioMes, fimMes);
        limiteValidator.validar(organizacaoId, TipoLimite.AGENDAMENTO, (int) (totalMesAtual + 1));

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));
        validarOrganizacao(organizacao.getId());

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        validarOrganizacao(cliente.getOrganizacao().getId());

        List<Servico> servicos = dto.getServicoIds().stream()
                .map(id -> servicoRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Serviço com ID " + id + " não encontrado.")))
                .collect(Collectors.toList());
        servicos.forEach(s -> validarOrganizacao(s.getOrganizacao().getId()));

        List<Funcionario> funcionarios = dto.getFuncionarioIds().stream()
                .map(id -> funcionarioRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado.")))
                .collect(Collectors.toList());
        funcionarios.forEach(f -> validarOrganizacao(f.getOrganizacao().getId()));

        // 2. Montar agendamento
        Agendamento agendamento = new Agendamento();
        agendamento.setOrganizacao(organizacao);
        agendamento.setCliente(cliente);
        agendamento.setServicos(servicos);
        agendamento.setFuncionarios(funcionarios);
        agendamento.setDtAgendamento(dto.getDtAgendamento());
        agendamento.setObservacao(dto.getObservacao());
        agendamento.setStatus(Status.PENDENTE);
        agendamento.setDtCriacao(LocalDateTime.now());

        // 3. Processar agendamento completo (validação, cobrança, evento)
        Agendamento agendamentoSalvo = processarAgendamento(agendamento);

        return new AgendamentoDTO(agendamentoSalvo);
    }


//    public List<AgendamentoDTO> getAllAgendamentos() {
//        List<Agendamento> agendamentos = agendamentoRepository.findAll();
//        return agendamentos.stream()
//                .map(AgendamentoDTO::new)
//                .collect(Collectors.toList());
//    }

    @Transactional
    public List<AgendamentoDTO> getAllAgendamentos() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        // Buscar apenas agendamentos da organização do usuário
        List<Agendamento> agendamentos = agendamentoRepository
                .findAllByClienteOrganizacaoId(organizacaoId);

        List<AgendamentoDTO> dtos = agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());

        enriquecerComNotificacoes(dtos, organizacaoId);
        return dtos;
    }

    public AgendamentoDTO getAgendamentoById(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento com ID " + id + " não encontrado."));

        Long organizacaoId = agendamento.getCliente().getOrganizacao().getId();
        validarOrganizacao(organizacaoId);

        AgendamentoDTO dto = new AgendamentoDTO(agendamento);
        enriquecerComNotificacoes(List.of(dto), organizacaoId);
        return dto;
    }

    private void enriquecerComNotificacoes(List<AgendamentoDTO> dtos, Long organizacaoId) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        List<Long> agendamentoIds = dtos.stream()
                .map(AgendamentoDTO::getId)
                .collect(Collectors.toList());

        List<NotificacaoEnviada> notificacoes = notificacaoEnviadaRepository
                .findByAgendamentoIdIn(agendamentoIds);

        Map<Long, List<NotificacaoEnviada>> porAgendamento = notificacoes.stream()
                .collect(Collectors.groupingBy(n -> n.getAgendamento().getId()));

        Map<String, ConfigNotificacao> configsPorChave = configNotificacaoRepository
                .findByOrganizacaoId(organizacaoId).stream()
                .collect(Collectors.toMap(
                        c -> chaveConfig(c.getTipo(), c.getHorasAntes()),
                        c -> c,
                        (a, b) -> a));

        for (AgendamentoDTO dto : dtos) {
            List<NotificacaoEnviada> lista = porAgendamento.getOrDefault(dto.getId(), List.of());
            List<NotificacaoEnviadaResumoDTO> resumos = lista.stream()
                    .map(n -> new NotificacaoEnviadaResumoDTO(
                            n,
                            configsPorChave.get(chaveConfig(n.getTipo(), n.getHorasAntes()))))
                    .collect(Collectors.toList());
            dto.setNotificacoes(resumos);
        }
    }

    private String chaveConfig(TipoNotificacao tipo, Integer horasAntes) {
        return tipo + "::" + horasAntes;
    }

    @Transactional
    public AgendamentoDTO updateAgendamento(Long id, AgendamentoUpdateDTO dto) {
        // 1. Buscar agendamento existente
        Agendamento agendamentoExistente = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento com ID " + id + " não encontrado."));

        // 2. Verificar se o agendamento pode ser alterado
        if (agendamentoExistente.getStatus() == Status.CONCLUIDO ||
                agendamentoExistente.getStatus() == Status.CANCELADO) {
            throw new IllegalArgumentException("Não é possível alterar um agendamento " +
                    agendamentoExistente.getStatus().getDescricao().toLowerCase() + ".");
        }

        // 3. Criar objeto temporário para validação se houver mudanças de horário/funcionário/serviço
        boolean precisaValidar = false;

        if (dto.getDtAgendamento() != null && !dto.getDtAgendamento().equals(agendamentoExistente.getDtAgendamento())) {
            precisaValidar = true;
            agendamentoExistente.setDtAgendamento(dto.getDtAgendamento());
        }

        if (dto.getServicoIds() != null && !dto.getServicoIds().isEmpty()) {
            List<Servico> novosServicos = dto.getServicoIds().stream()
                    .map(servicoId -> servicoRepository.findById(servicoId)
                            .orElseThrow(() -> new IllegalArgumentException("Serviço com ID " + servicoId + " não encontrado.")))
                    .collect(Collectors.toList());

            // Verificar se houve mudança nos serviços
            if (!agendamentoExistente.getServicos().equals(novosServicos)) {
                precisaValidar = true;
                agendamentoExistente.setServicos(novosServicos);
                // Re-derivar questionários a partir dos novos serviços para manter consistência.
                vincularQuestionariosDosServicos(agendamentoExistente);
            }
        }

        if (dto.getFuncionarioIds() != null && !dto.getFuncionarioIds().isEmpty()) {
            List<Funcionario> novosFuncionarios = dto.getFuncionarioIds().stream()
                    .map(funcionarioId -> funcionarioRepository.findById(funcionarioId)
                            .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + funcionarioId + " não encontrado.")))
                    .collect(Collectors.toList());

            // Verificar se houve mudança nos funcionários
            if (!agendamentoExistente.getFuncionarios().equals(novosFuncionarios)) {
                precisaValidar = true;
                agendamentoExistente.setFuncionarios(novosFuncionarios);
            }
        }

        // 4. Se houve mudanças que afetam disponibilidade, validar novamente
        if (precisaValidar) {
            // Remover bloqueios antigos do agendamento
            if (agendamentoExistente.getBloqueioAgenda() != null) {
                disponibilidadeRepository.delete(agendamentoExistente.getBloqueioAgenda());
            }

            // Validar nova configuração
            validarDisponibilidadeAgendamento(agendamentoExistente);

            // Criar novos bloqueios
            criarBloqueiosAgenda(agendamentoExistente);

            // Atualizar cobrança se necessário
            atualizarCobrancaAgendamento(agendamentoExistente);
        }

        // 5. Atualizar campos simples
        if (dto.getObservacao() != null) {
            agendamentoExistente.setObservacao(dto.getObservacao());
        }

        if (dto.getStatus() != null) {
            try {
                Status novoStatus = Status.valueOf(dto.getStatus().toUpperCase());
                alterarStatusAgendamento(agendamentoExistente, novoStatus);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Status inválido: " + dto.getStatus());
            }
        }

        agendamentoExistente.setDtAtualizacao(LocalDateTime.now());

        Agendamento agendamentoAtualizado = agendamentoRepository.save(agendamentoExistente);
        return new AgendamentoDTO(agendamentoAtualizado);
    }

    @Transactional
    public void cancelAgendamento(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento com ID " + id + " não encontrado."));

        validarOrganizacao(agendamento.getCliente().getOrganizacao().getId());

        // Verificar se agendamento pode ser cancelado
        if (agendamento.getStatus() == Status.CONCLUIDO) {
            throw new IllegalArgumentException("Não é possível cancelar um agendamento já concluído.");
        }

        // Cancelar agendamento
        agendamento.cancelarAgendamento();
        agendamento.setDtAtualizacao(LocalDateTime.now());

        // Remover bloqueios da agenda
        if (agendamento.getBloqueioAgenda() != null) {
            disponibilidadeRepository.delete(agendamento.getBloqueioAgenda());
        }

        // Cancelar cobrança associada se existir e estiver pendente
//        if (agendamento.getCobrancas() != null &&
//                agendamento.getCobrancas().getStatusCobranca() != Cobranca.StatusCobranca.PAGO) {
//
//            agendamento.getCobranca().cancelar();
//            cobrancaRepository.save(agendamento.getCobranca());
//        }

        agendamentoRepository.save(agendamento);

        // Publicar evento de agendamento cancelado
        List<Long> funcionarioIds = agendamento.getFuncionarios().stream()
                .map(Funcionario::getId)
                .collect(Collectors.toList());
        String nomeServicosCancelado = agendamento.getServicos().stream()
                .map(Servico::getNome).collect(Collectors.joining(", "));
        String nomeProfCancelado = agendamento.getFuncionarios().stream()
                .map(Funcionario::getNomeCompleto).collect(Collectors.joining(", "));
        eventPublisher.publishEvent(new AgendamentoCanceladoEvent(
                this,
                agendamento.getId(),
                agendamento.getCliente().getId(),
                agendamento.getCliente().getNomeCompleto(),
                funcionarioIds,
                agendamento.getCliente().getOrganizacao().getId(),
                agendamento.getDtAgendamento(),
                nomeServicosCancelado,
                nomeProfCancelado
        ));
    }

    @Transactional
    public AgendamentoDTO updateStatusAgendamento(Long id, String status) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento com ID " + id + " não encontrado."));

        validarOrganizacao(agendamento.getCliente().getOrganizacao().getId());

        Status novoStatus;
        try {
            novoStatus = Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: " + status);
        }

        // Validar se a transição de status é permitida
        validarTransicaoStatus(agendamento.getStatus(), novoStatus);

        alterarStatusAgendamento(agendamento, novoStatus);
        agendamento.setDtAtualizacao(LocalDateTime.now());

        Agendamento agendamentoAtualizado = agendamentoRepository.save(agendamento);
        return new AgendamentoDTO(agendamentoAtualizado);
    }

    private void validarTransicaoStatus(Status statusAtual, Status novoStatus) {
        // Se já está no status desejado, não precisa validar
        if (statusAtual == novoStatus) {
            return;
        }

        List<Status> transicoesPermitidas = STATUS_TRANSITIONS.getOrDefault(statusAtual, List.of());

        if (!transicoesPermitidas.contains(novoStatus)) {
            throw new IllegalArgumentException(
                String.format("Transição de status inválida: não é possível mudar de %s para %s. " +
                    "Transições permitidas: %s",
                    statusAtual.getDescricao(),
                    novoStatus.getDescricao(),
                    transicoesPermitidas.isEmpty() ? "nenhuma (estado final)" :
                        transicoesPermitidas.stream()
                            .map(Status::getDescricao)
                            .collect(Collectors.joining(", "))
                )
            );
        }
    }

    private void validarDisponibilidadeAgendamento(Agendamento agendamento) {
        // Validar bloqueio da organização (feriados/bloqueios)
        validarBloqueioOrganizacao(agendamento.getOrganizacao().getId(),
                agendamento.getDtAgendamento().toLocalDate());

        // Calcular duração total dos serviços
        int duracaoTotalMinutos = agendamento.getServicos().stream()
                .mapToInt(Servico::getTempoEstimadoMinutos)
                .sum();

        LocalDateTime dataHoraFimAgendamento = agendamento.getDtAgendamento().plusMinutes(duracaoTotalMinutos);

        // Validar para cada funcionário
        for (Funcionario funcionario : agendamento.getFuncionarios()) {
            validarDisponibilidade(funcionario, agendamento.getDtAgendamento(), dataHoraFimAgendamento, duracaoTotalMinutos);
        }
    }

    private void criarBloqueiosAgenda(Agendamento agendamento) {
        int duracaoTotalMinutos = agendamento.getServicos().stream()
                .mapToInt(Servico::getTempoEstimadoMinutos)
                .sum();

        LocalDateTime dataHoraFimAgendamento = agendamento.getDtAgendamento().plusMinutes(duracaoTotalMinutos);

        // Criar bloqueios para cada funcionário
        for (Funcionario funcionario : agendamento.getFuncionarios()) {
            BloqueioAgenda bloqueio = new BloqueioAgenda(
                    funcionario,
                    agendamento.getDtAgendamento(),
                    dataHoraFimAgendamento,
                    "Agendamento de Serviço",
                    TipoBloqueio.AGENDAMENTO,
                    agendamento
            );
            disponibilidadeRepository.save(bloqueio);
        }
    }

    private void atualizarBloqueiosAgenda(Agendamento agendamento) {
        int duracaoTotalMinutos = agendamento.getServicos().stream()
                .mapToInt(Servico::getTempoEstimadoMinutos)
                .sum();



        LocalDateTime dataHoraFimAgendamento = agendamento.getDtAgendamento().plusMinutes(duracaoTotalMinutos);

        // Criar bloqueios para cada funcionário
        for (Funcionario funcionario : agendamento.getFuncionarios()) {

            BloqueioAgenda bloqueioAgenda = disponibilidadeRepository.getById(agendamento.getBloqueioAgenda().getId());

//            BloqueioAgenda bloqueio = new BloqueioAgenda(
//                    funcionario,
//                    agendamento.getDtAgendamento(),
//                    dataHoraFimAgendamento,
//                    "Agendamento de Serviço",
//                    TipoBloqueio.AGENDAMENTO,
//                    agendamento
//            );
            bloqueioAgenda.setInicioBloqueio(agendamento.getDtAgendamento());
            bloqueioAgenda.setFimBloqueio(dataHoraFimAgendamento);

            disponibilidadeRepository.save(bloqueioAgenda);
        }
    }

    private void atualizarCobrancaAgendamento(Agendamento agendamento) {
        if (agendamento.getCobrancas() != null) {
            Cobranca cobranca = agendamento.getCobrancas().get(0);

            // Só permitir atualização se cobrança não estiver paga
            if (!cobranca.isPaga()) {
                // Recalcular valor total dos serviços
                BigDecimal novoValorTotal = agendamento.getServicos().stream()
                        .map(Servico::getPreco)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                cobranca.setValor(novoValorTotal);
                cobranca.setDtVencimento(agendamento.getDtAgendamento().toLocalDate());
                cobranca.setDtAtualizacao(LocalDateTime.now());

                // Recalcular valores pendentes
                cobranca.recalcularValores();

                cobrancaRepository.save(cobranca);
            } else {
                throw new IllegalStateException("Não é possível alterar agendamento com cobrança já paga. Realize estorno primeiro.");
            }
        }
    }

    private void alterarStatusAgendamento(Agendamento agendamento, Status novoStatus) {
        switch (novoStatus) {
            case PENDENTE:
                agendamento.setStatus(Status.PENDENTE);
                break;

            case AGENDADO:
                agendamento.marcarComoAgendado();
                break;

            case AGUARDANDO_CONFIRMACAO:
                agendamento.setStatus(Status.AGUARDANDO_CONFIRMACAO);
                // Enviar notificação solicitando confirmação
                break;

            case CONFIRMADO:
                agendamento.setStatus(Status.CONFIRMADO);
                agendamento.setDtConfirmacao(LocalDateTime.now());
                // Publicar evento de confirmacao
                List<Long> funcIds = agendamento.getFuncionarios().stream()
                        .map(Funcionario::getId)
                        .collect(Collectors.toList());
                String nomeServicosConf = agendamento.getServicos().stream()
                        .map(Servico::getNome).collect(Collectors.joining(", "));
                String nomeProfConf = agendamento.getFuncionarios().stream()
                        .map(Funcionario::getNomeCompleto).collect(Collectors.joining(", "));
                eventPublisher.publishEvent(new AgendamentoConfirmadoEvent(
                        this,
                        agendamento.getId(),
                        agendamento.getCliente().getId(),
                        agendamento.getCliente().getNomeCompleto(),
                        funcIds,
                        agendamento.getCliente().getOrganizacao().getId(),
                        agendamento.getDtAgendamento(),
                        nomeServicosConf,
                        nomeProfConf
                ));
                break;

            case EM_ESPERA:
                agendamento.colocarEmEspera();
                break;

            case EM_ANDAMENTO:
                agendamento.setStatus(Status.EM_ANDAMENTO);
                break;

            case CONCLUIDO:
                agendamento.marcarComoConcluido();
                // NÃO marcar cobrança como paga automaticamente
                // Isso deve ser feito através do processo de pagamento
                break;

            case CANCELADO:
                agendamento.cancelarAgendamento();

                // Remover bloqueios da agenda
                if (agendamento.getBloqueioAgenda() != null) {
                    disponibilidadeRepository.delete(agendamento.getBloqueioAgenda());
                    agendamento.setBloqueioAgenda(null);
                }

                // Cancelar cobranças pendentes
                if (agendamento.getCobrancas() != null) {
                    agendamento.getCobrancas().stream()
                        .filter(c -> !c.isPaga())
                        .forEach(c -> {
                            c.cancelar();
                            cobrancaRepository.save(c);
                        });
                }

                // Publicar evento de agendamento cancelado
                List<Long> cancelFuncIds = agendamento.getFuncionarios().stream()
                        .map(Funcionario::getId)
                        .collect(Collectors.toList());
                String nomeServicosCancel2 = agendamento.getServicos().stream()
                        .map(Servico::getNome).collect(Collectors.joining(", "));
                String nomeProfCancel2 = agendamento.getFuncionarios().stream()
                        .map(Funcionario::getNomeCompleto).collect(Collectors.joining(", "));
                eventPublisher.publishEvent(new AgendamentoCanceladoEvent(
                        this,
                        agendamento.getId(),
                        agendamento.getCliente().getId(),
                        agendamento.getCliente().getNomeCompleto(),
                        cancelFuncIds,
                        agendamento.getCliente().getOrganizacao().getId(),
                        agendamento.getDtAgendamento(),
                        nomeServicosCancel2,
                        nomeProfCancel2
                ));
                break;

            case REAGENDADO:
                agendamento.setStatus(Status.REAGENDADO);
                // Preparar para novo agendamento - usuário deverá definir nova data
                break;

            case NAO_COMPARECEU:
                agendamento.setStatus(Status.NAO_COMPARECEU);
                // Aplicar taxa de não comparecimento se configurada
                aplicarTaxaNaoComparecimento(agendamento);
                break;

            case VENCIDA:
                agendamento.setStatus(Status.VENCIDA);
                // Cobrança vencida - pode enviar notificação
                break;

            case PAGO:
                agendamento.setStatus(Status.PAGO);
                // Todas as cobranças foram pagas
                break;

            default:
                agendamento.setStatus(novoStatus);
        }
    }

    private void aplicarTaxaNaoComparecimento(Agendamento agendamento) {
        // Verificar se existe política de taxa para não comparecimento
        // Por agora, só registrar o status - a taxa pode ser implementada depois
        agendamento.setObservacao(
                (agendamento.getObservacao() != null ? agendamento.getObservacao() + " | " : "") +
                        "Cliente não compareceu - " + LocalDateTime.now().toString()
        );
    }

    public Agendamento salvar(Agendamento agendamento) {
        return agendamentoRepository.save(agendamento);
    }

    public Agendamento buscar(Long id) {
        return agendamentoRepository.findById(id).orElse(null);
    }

    public Agendamento atualizar(Agendamento agendamento) {
        return agendamentoRepository.save(agendamento);
    }


    //@Transactional
//    public Agendamento criarAgendamento(Agendamento novoAgendamento) {
//        // 1. Validar a Data e Hora do Agendamento
//        if (novoAgendamento.getDtAgendamento().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Não é possível agendar para o passado.");
//        }
//
//        // 2. Calcular a Duração Total dos Serviços
//        int duracaoTotalMinutos = novoAgendamento.getServicos().stream()
//                .mapToInt(Servico::getTempoEstimadoMinutos)
//                .sum();
//
//        // 3. Definir o Horário de Término do Agendamento
//        LocalDateTime dataHoraFimAgendamento = novoAgendamento.getDtAgendamento().plusMinutes(duracaoTotalMinutos);
//
//        // 4. Para cada funcionário envolvido no agendamento:
//        for (Funcionario funcionario : novoAgendamento.getFuncionarios()) {
//            // a. Verificar a Jornada de Trabalho do Funcionário
//            DayOfWeek diaSemanaAgendamento = novoAgendamento.getDtAgendamento().getDayOfWeek();
//            LocalTime horaInicioAgendamento = novoAgendamento.getDtAgendamento().toLocalTime();
//            LocalTime horaFimAgendamento = dataHoraFimAgendamento.toLocalTime();
//
//            // Encontre a jornada de trabalho para o dia específico
//            JornadaTrabalho jornada = jornadaTrabalhoRepository.findByFuncionarioAndDiaSemana(
//                    funcionario,
//                    DiaSemana.fromDayOfWeek(diaSemanaAgendamento) // <- Use o método de conversão
//            ).orElseThrow(() ->
//                    new RuntimeException("Funcionário não tem jornada de trabalho definida para este dia.")
//            );
//
//            // Verifica se o agendamento está dentro da jornada
//            if (horaInicioAgendamento.isBefore(jornada.getHoraInicio()) ||
//                    horaFimAgendamento.isAfter(jornada.getHoraFim())) {
//                throw new IllegalArgumentException("Funcionário " + funcionario.getNomeCompleto() + " não está disponível neste horário de acordo com sua jornada.");
//            }
//
//            // b. Verificar Conflitos de Bloqueio na Agenda do Funcionário
//            List<BloqueioAgenda> bloqueiosConflitantes = disponibilidadeRepository.findByFuncionarioAndInicioBloqueioBetween(
//                    funcionario,
//                    novoAgendamento.getDtAgendamento(),
//                    dataHoraFimAgendamento
//            );
//
//            // Filtra bloqueios que realmente se sobrepõem
//            boolean temConflito = bloqueiosConflitantes.stream().anyMatch(bloqueio ->
//                    (novoAgendamento.getDtAgendamento().isBefore(bloqueio.getFimBloqueio()) &&
//                            dataHoraFimAgendamento.isAfter(bloqueio.getInicioBloqueio()))
//            );
//
//            if (temConflito) {
//                throw new IllegalArgumentException("Funcionário " + funcionario.getNomeCompleto() + " já possui um bloqueio na agenda neste período.");
//            }
//        }
//
//        // 5. Se todas as validações passarem, salve o agendamento
//        Agendamento agendamentoSalvo = agendamentoRepository.save(novoAgendamento);
//
//        // 6. Crie os Bloqueios de Agenda para os funcionários envolvidos
//        for (Funcionario funcionario : novoAgendamento.getFuncionarios()) {
//            BloqueioAgenda bloqueio = new BloqueioAgenda(
//                    funcionario,
//                    novoAgendamento.getDtAgendamento(),
//                    dataHoraFimAgendamento,
//                    "Agendamento de Serviço",
//                    TipoBloqueio.AGENDAMENTO,
//                    agendamentoSalvo
//            );
//            disponibilidadeRepository.save(bloqueio);
//        }
//
//        return agendamentoSalvo;
//    }

    @Transactional
    public Agendamento criarAgendamento(Agendamento novoAgendamento) {
        // 1. Validar a Data e Hora do Agendamento
        if (novoAgendamento.getDtAgendamento().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível agendar para o passado.");
        }

        // 1.1 Validar se o dia está bloqueado na organização (feriados/bloqueios)
        validarBloqueioOrganizacao(novoAgendamento.getOrganizacao().getId(),
                novoAgendamento.getDtAgendamento().toLocalDate());

        // 2. Calcular a Duração Total dos Serviços
        int duracaoTotalMinutos = novoAgendamento.getServicos().stream()
                .mapToInt(Servico::getTempoEstimadoMinutos)
                .sum();

        // 3. Definir o Horário de Término do Agendamento
        LocalDateTime dataHoraFimAgendamento = novoAgendamento.getDtAgendamento().plusMinutes(duracaoTotalMinutos+15);

        // 4. Para cada funcionário envolvido no agendamento, validar disponibilidade
        for (Funcionario funcionario : novoAgendamento.getFuncionarios()) {
            validarDisponibilidade(funcionario, novoAgendamento.getDtAgendamento(), dataHoraFimAgendamento, duracaoTotalMinutos);
        }

        // 5. Se todas as validações passarem, salve o agendamento
        Agendamento agendamentoSalvo = agendamentoRepository.save(novoAgendamento);

        // 6. Crie os Bloqueios de Agenda para os funcionários envolvidos
        for (Funcionario funcionario : novoAgendamento.getFuncionarios()) {
            BloqueioAgenda bloqueio = new BloqueioAgenda(
                    funcionario,
                    novoAgendamento.getDtAgendamento(),
                    dataHoraFimAgendamento,
                    "Agendamento de Serviço",
                    TipoBloqueio.AGENDAMENTO,
                    agendamentoSalvo
            );
            disponibilidadeRepository.save(bloqueio);
        }

        return agendamentoSalvo;
    }

    @Transactional
    public Agendamento concluirAgendamento(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));

        agendamento.marcarComoConcluido();
        return agendamentoRepository.save(agendamento);
    }

    /**
     * Valida se a data está bloqueada por feriado ou bloqueio da organização
     */
    private void validarBloqueioOrganizacao(Long organizacaoId, LocalDate data) {
        List<BloqueioOrganizacao> bloqueios = bloqueioOrganizacaoRepository
                .findBloqueiosAtivosNaData(organizacaoId, data);

        if (!bloqueios.isEmpty()) {
            BloqueioOrganizacao primeiro = bloqueios.get(0);
            String motivo = primeiro.getTitulo();
            if (primeiro.getDescricao() != null && !primeiro.getDescricao().isEmpty()) {
                motivo += " - " + primeiro.getDescricao();
            }
            throw new IllegalArgumentException(
                    "Não é possível agendar para o dia " + data +
                    ". Motivo: " + motivo);
        }
    }

    private void validarDisponibilidade(Funcionario funcionario, LocalDateTime inicioAgendamento, LocalDateTime fimAgendamento, int duracaoTotalMinutos) {
        DayOfWeek diaSemanaAgendamento = inicioAgendamento.getDayOfWeek();
        LocalTime horaInicioAgendamento = inicioAgendamento.toLocalTime();
        LocalTime horaFimAgendamento = fimAgendamento.toLocalTime();

        // 1. Verificar a Jornada de Trabalho (NOVO MODELO)
        DiaSemana diaSemanaEnum = DiaSemana.fromDayOfWeek(diaSemanaAgendamento);

        JornadaDia jornadaDia = funcionario.getJornadasDia().stream()
                .filter(j -> j.getDiaSemana().equals(diaSemanaEnum) && j.getAtivo())
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Funcionário " + funcionario.getNomeCompleto() + " não tem jornada de trabalho ativa para " + diaSemanaEnum.getDescricao() + "."
                ));

        // Verificar se há horários de trabalho definidos
        if (jornadaDia.getHorarios() == null || jornadaDia.getHorarios().isEmpty()) {
            throw new RuntimeException(
                    "Funcionário " + funcionario.getNomeCompleto() + " não possui horários de trabalho definidos para " + diaSemanaEnum.getDescricao() + "."
            );
        }

        // 2. Validar se o agendamento está dentro de algum período de trabalho
        boolean dentroDeAlgumPeriodo = jornadaDia.getHorarios().stream()
                .anyMatch(horario -> {
                    LocalTime horaInicioTrabalho = horario.getHoraInicio();
                    LocalTime horaFimTrabalho = horario.getHoraFim();

                    // O agendamento deve começar e terminar dentro do mesmo período
                    return !horaInicioAgendamento.isBefore(horaInicioTrabalho)
                            && !horaFimAgendamento.isAfter(horaFimTrabalho);
                });

        if (!dentroDeAlgumPeriodo) {
            // Montar mensagem com os períodos disponíveis
            String periodosDisponiveis = jornadaDia.getHorarios().stream()
                    .map(h -> h.getHoraInicio() + " às " + h.getHoraFim())
                    .collect(Collectors.joining(", "));

            throw new IllegalArgumentException(
                    "Funcionário " + funcionario.getNomeCompleto() +
                            " não está disponível neste horário. Períodos de trabalho: " + periodosDisponiveis
            );
        }

        // 3. Verificar conflitos com agendamentos existentes
        LocalDateTime inicioDia = inicioAgendamento.toLocalDate().atStartOfDay();
        LocalDateTime fimDia = inicioAgendamento.toLocalDate().atTime(23, 59, 59);

        List<Agendamento> agendamentosAtivos = agendamentoRepository
                .findAtivosByFuncionarioAndOrganizacaoAndPeriodo(
                        funcionario.getId(),
                        funcionario.getOrganizacao().getId(),
                        inicioDia, fimDia);

        boolean conflitoAgendamento = agendamentosAtivos.stream().anyMatch(ag -> {
            LocalDateTime agInicio = ag.getDtAgendamento();
            int agDuracao = ag.getServicos() != null
                    ? ag.getServicos().stream().mapToInt(Servico::getTempoEstimadoMinutos).sum()
                    : 30;
            LocalDateTime agFim = agInicio.plusMinutes(agDuracao);
            return inicioAgendamento.isBefore(agFim) && fimAgendamento.isAfter(agInicio);
        });

        if (conflitoAgendamento) {
            throw new IllegalArgumentException(
                    "Funcionário " + funcionario.getNomeCompleto() +
                            " já possui um agendamento neste horário."
            );
        }

        // 4. Verificar conflitos com bloqueios manuais (férias, intervalos, etc.)
        List<BloqueioAgenda> bloqueiosConflitantes = disponibilidadeRepository
                .findByFuncionarioAndInicioBloqueioBetween(
                        funcionario,
                        inicioDia,
                        inicioAgendamento.toLocalDate().plusDays(1).atStartOfDay()
                );

        boolean temBloqueioManual = bloqueiosConflitantes.stream()
                .filter(bloqueio -> !bloqueio.getTipoBloqueio().equals(TipoBloqueio.AGENDAMENTO))
                .anyMatch(bloqueio ->
                        (inicioAgendamento.isBefore(bloqueio.getFimBloqueio()) &&
                                fimAgendamento.isAfter(bloqueio.getInicioBloqueio()))
                );

        if (temBloqueioManual) {
            throw new IllegalArgumentException(
                    "Funcionário " + funcionario.getNomeCompleto() +
                            " já possui um bloqueio na agenda neste período."
            );
        }
    }

    @Transactional
    public List<HorarioDisponivelResponse> getHorariosDisponiveis(DisponibilidadeRequest request) {
        // 1. Recuperar o Funcionário
        Funcionario funcionario = funcionarioRepository.findById(request.getFuncionarioId())
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado."));

        ConfigSistema config = TenantContext.getCurrentConfigSistema();
        // 2. Validar organização
        validarOrganizacao(request.getOrganizacaoId());

        // 2.1 Verificar se o dia está bloqueado na organização (feriados/bloqueios)
        List<BloqueioOrganizacao> bloqueiosOrg = bloqueioOrganizacaoRepository
                .findBloqueiosAtivosNaData(request.getOrganizacaoId(), request.getDataDesejada());
        if (!bloqueiosOrg.isEmpty()) {
            return new ArrayList<>(); // Dia bloqueado, sem horários disponíveis
        }

        // 3. Calcular a Duração Total dos Serviços + Tolerância
        int duracaoServicos = request.getServicoIds().stream()
                .map(servicoRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .mapToInt(Servico::getTempoEstimadoMinutos)
                .sum();

        int duracaoTotalNecessaria = duracaoServicos + config.getConfigAgendamento().getToleranciaAgendamento();

        // 4. Obter a JornadaDia para o dia da semana desejado
        DayOfWeek diaDaSemana = request.getDataDesejada().getDayOfWeek();
        DiaSemana diaSemanaEnum = DiaSemana.fromDayOfWeek(diaDaSemana);

        JornadaDia jornadaDia = funcionario.getJornadasDia().stream()
                .filter(j -> j.getDiaSemana().equals(diaSemanaEnum) && j.getAtivo())
                .findFirst()
                .orElse(null);

        // Se não há jornada ativa para o dia, retorna vazio
        if (jornadaDia == null || jornadaDia.getHorarios() == null || jornadaDia.getHorarios().isEmpty()) {
            return new ArrayList<>();
        }

        // 5. Obter TODOS os bloqueios para o funcionário no dia (INCLUINDO AGENDAMENTOS)
        LocalDateTime inicioDoDia = request.getDataDesejada().atStartOfDay();
        LocalDateTime fimDoDia = request.getDataDesejada().plusDays(1).atStartOfDay().minusNanos(1);

        List<BloqueioAgenda> bloqueios = disponibilidadeRepository.findByFuncionarioAndInicioBloqueioBetween(
                funcionario, inicioDoDia, fimDoDia);

        // INCLUIR TODOS os bloqueios (incluindo AGENDAMENTO)
        List<BloqueioAgenda> bloqueiosValidos = new ArrayList<>(bloqueios);

        // ✅ NOVO: Adicionar bloqueio para horários no passado se for o dia atual
        LocalDateTime agora = LocalDateTime.now();
        boolean isDiaAtual = request.getDataDesejada().isEqual(agora.toLocalDate());

        if (isDiaAtual) {
            // Criar bloqueio virtual do início do dia até o momento atual
            bloqueiosValidos.add(new BloqueioAgenda(
                    funcionario,
                    inicioDoDia,
                    agora,
                    "Horário já passou",
                    TipoBloqueio.OUTRO,
                    null
            ));
        }

        // 6. Processar cada período de trabalho do dia
        List<HorarioDisponivelResponse> horariosDisponiveis = new ArrayList<>();

        for (HorarioTrabalho horarioTrabalho : jornadaDia.getHorarios()) {
            LocalDateTime inicioTrabalho = request.getDataDesejada().atTime(horarioTrabalho.getHoraInicio());
            LocalDateTime fimTrabalho = request.getDataDesejada().atTime(horarioTrabalho.getHoraFim());

            // 7. Criar lista de bloqueios para este período específico
            List<BloqueioAgenda> bloqueiosDoPeriodo = new ArrayList<>();

            // Adicionar bloqueio ANTES do início do trabalho (desde o início do dia)
            bloqueiosDoPeriodo.add(new BloqueioAgenda(
                    funcionario,
                    inicioDoDia,
                    inicioTrabalho,
                    "Fora da Jornada (Antes)",
                    TipoBloqueio.OUTRO,
                    null
            ));

            // Adicionar bloqueios reais que estão dentro ou próximos deste período
            bloqueiosValidos.forEach(bloqueio -> {
                LocalDateTime inicioBloqueio = bloqueio.getInicioBloqueio().isBefore(inicioDoDia)
                        ? inicioDoDia : bloqueio.getInicioBloqueio();
                LocalDateTime fimBloqueio = bloqueio.getFimBloqueio().isAfter(fimDoDia)
                        ? fimDoDia : bloqueio.getFimBloqueio();

                // Adiciona apenas se o bloqueio intercepta este período de trabalho
                if (inicioBloqueio.isBefore(fimTrabalho) && fimBloqueio.isAfter(inicioTrabalho)) {
                    bloqueiosDoPeriodo.add(new BloqueioAgenda(
                            funcionario,
                            inicioBloqueio,
                            fimBloqueio,
                            bloqueio.getDescricao(),
                            bloqueio.getTipoBloqueio(),
                            null
                    ));
                }
            });

            // Adicionar bloqueio DEPOIS do fim do trabalho (até o fim do dia)
            bloqueiosDoPeriodo.add(new BloqueioAgenda(
                    funcionario,
                    fimTrabalho,
                    fimDoDia,
                    "Fora da Jornada (Depois)",
                    TipoBloqueio.OUTRO,
                    null
            ));

            // Ordenar bloqueios
            bloqueiosDoPeriodo.sort(Comparator.comparing(BloqueioAgenda::getInicioBloqueio));

            // 8. Encontrar horários disponíveis entre os bloqueios
            LocalDateTime ponteiroAtual = inicioTrabalho;

            for (BloqueioAgenda bloqueio : bloqueiosDoPeriodo) {
                LocalDateTime inicioBloqueioAtual = bloqueio.getInicioBloqueio();
                LocalDateTime fimBloqueioAtual = bloqueio.getFimBloqueio();

                // Verificar se há espaço antes do bloqueio
                if (ponteiroAtual.isBefore(inicioBloqueioAtual)) {
                    long duracaoGapMinutos = java.time.Duration.between(ponteiroAtual, inicioBloqueioAtual).toMinutes();

                    // Gerar slots disponíveis neste gap
                    while (duracaoGapMinutos >= duracaoTotalNecessaria) {
                        LocalDateTime slotFim = ponteiroAtual.plusMinutes(duracaoServicos);

                        if (slotFim.isAfter(inicioBloqueioAtual) || slotFim.isAfter(fimTrabalho)) {
                            break;
                        }

                        if (slotFim.toLocalDate().isEqual(request.getDataDesejada())) {
                            horariosDisponiveis.add(new HorarioDisponivelResponse(
                                    ponteiroAtual.toLocalTime(),
                                    slotFim.toLocalTime(),
                                    ponteiroAtual.toLocalTime() + " - " + slotFim.toLocalTime()
                            ));
                        }

                        ponteiroAtual = slotFim.plusMinutes(config.getConfigAgendamento().getToleranciaAgendamento());
                        duracaoGapMinutos = java.time.Duration.between(ponteiroAtual, inicioBloqueioAtual).toMinutes();
                    }
                }

                // Avançar o ponteiro para o fim do bloqueio
                ponteiroAtual = ponteiroAtual.isAfter(fimBloqueioAtual) ? ponteiroAtual : fimBloqueioAtual;
            }

            // 9. Verificar se há espaço disponível após o último bloqueio até o fim da jornada
            if (ponteiroAtual.isBefore(fimTrabalho)) {
                long duracaoFinalGapMinutos = java.time.Duration.between(ponteiroAtual, fimTrabalho).toMinutes();

                while (duracaoFinalGapMinutos >= duracaoTotalNecessaria) {
                    LocalDateTime slotFim = ponteiroAtual.plusMinutes(duracaoServicos);

                    if (slotFim.isAfter(fimTrabalho)) {
                        break;
                    }

                    horariosDisponiveis.add(new HorarioDisponivelResponse(
                            ponteiroAtual.toLocalTime(),
                            slotFim.toLocalTime(),
                            ponteiroAtual.toLocalTime() + " - " + slotFim.toLocalTime()
                    ));

                    ponteiroAtual = slotFim.plusMinutes(config.getConfigAgendamento().getToleranciaAgendamento());
                    duracaoFinalGapMinutos = java.time.Duration.between(ponteiroAtual, fimTrabalho).toMinutes();
                }
            }
        }

        // 10. Ordenar e retornar os horários disponíveis
        return horariosDisponiveis.stream()
                .sorted(Comparator.comparing(HorarioDisponivelResponse::getHoraInicio))
                .distinct() // Remove possíveis duplicatas entre períodos
                .collect(Collectors.toList());
    }

    public List<AgendamentoDTO> getAgendamentosComCobrancasPendentes() {
        List<Agendamento> agendamentos = agendamentoRepository.findAll().stream()
                .filter(a -> {
                    if (a.getCobrancas() == null || a.getCobrancas().isEmpty()) {
                        return false;
                    }

                    return a.getCobrancas().stream()
                            .anyMatch(c -> c.isPendente() &&
                                    (c.isSinal() || c.isRestante() || c.isIntegral()));
                })
                .sorted(Comparator.comparing(Agendamento::getDtAgendamento))
                .collect(Collectors.toList());

        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    public List<AgendamentoDTO> getAgendamentosVencidos() {
        LocalDate hoje = LocalDate.now();

        List<Agendamento> agendamentos = agendamentoRepository.findAll().stream()
                .filter(a -> {
                    if (a.getCobrancas() == null || a.getCobrancas().isEmpty()) {
                        return false;
                    }

                    return a.getCobrancas().stream()
                            .anyMatch(c -> c.getDtVencimento() != null &&
                                    c.getDtVencimento().isBefore(hoje) &&
                                    c.isPendente() &&
                                    (c.isSinal() || c.isIntegral()));
                })
                .sorted(Comparator.comparing(a ->
                        a.getCobrancas().stream()
                                .filter(c -> c.getDtVencimento() != null &&
                                        c.isPendente() &&
                                        (c.isSinal() || c.isIntegral()))
                                .map(Cobranca::getDtVencimento)
                                .min(LocalDate::compareTo)
                                .orElse(LocalDate.MAX)
                ))
                .collect(Collectors.toList());

        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    public List<AgendamentoDTO> getAgendamentosByCliente(Long clienteId) {

        Long organizacaoId = getOrganizacaoIdFromContext();

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente com ID " + clienteId + " não encontrado."));

        validarOrganizacao(cliente.getOrganizacao().getId());

//        List<Agendamento> agendamentos = agendamentoRepository.findByCliente(cliente);

        List<Agendamento> agendamentos = agendamentoRepository.findByClienteIdAndClienteOrganizacaoId(clienteId, organizacaoId);

        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    // Método para buscar agendamentos por funcionário
    public List<AgendamentoDTO> getAgendamentosByFuncionario(Long funcionarioId) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + funcionarioId + " não encontrado."));

        validarOrganizacao(funcionario.getOrganizacao().getId());

//        List<Agendamento> agendamentos = agendamentoRepository.findByFuncionariosContaining(funcionario);

        List<Agendamento> agendamentos = agendamentoRepository.findByFuncionariosIdAndClienteOrganizacaoId(funcionarioId, organizacaoId);

        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    // Método para buscar agendamentos por data
    public List<AgendamentoDTO> getAgendamentosByData(LocalDate data) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.plusDays(1).atStartOfDay(); // MUDANÇA: próximo dia às 00:00:00

        List<Agendamento> agendamentos = agendamentoRepository.findByDtAgendamentoBetweenAndClienteOrganizacaoId(inicioDia, fimDia, organizacaoId);

        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    // Método para buscar agendamentos por status
    public List<AgendamentoDTO> getAgendamentosByStatus(String status) {
        try {
            Status statusEnum = Status.valueOf(status.toUpperCase());
            List<Agendamento> agendamentos = agendamentoRepository.findByStatus(statusEnum);
            return agendamentos.stream()
                    .map(AgendamentoDTO::new)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: " + status);
        }
    }

    // Método para buscar agendamentos de uma data específica para um funcionário
    public List<AgendamentoDTO> getAgendamentosByFuncionarioAndData(Long funcionarioId, LocalDate data) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + funcionarioId + " não encontrado."));

        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(23, 59, 59);

        List<Agendamento> agendamentos = agendamentoRepository
                .findByFuncionariosContainingAndDtAgendamentoBetween(funcionario, inicioDia, fimDia);

        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    public List<AgendamentoDTO> getAgendamentosProximos() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime proximosSete = agora.plusDays(7);

        List<Agendamento> agendamentos = agendamentoRepository.findByDtAgendamentoBetween(agora, proximosSete);
        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .sorted((a1, a2) -> a1.getDtAgendamento().compareTo(a2.getDtAgendamento()))
                .collect(Collectors.toList());
    }

    // Método para buscar agendamentos do dia atual
    public List<AgendamentoDTO> getAgendamentosHoje() {
        return getAgendamentosByData(LocalDate.now());
    }

    // Método para obter estatísticas de agendamentos
    public AgendamentoEstatisticasDTO getEstatisticasAgendamentos() {
        long totalAgendamentos = agendamentoRepository.count();
        long agendamentosPendentes = agendamentoRepository.countByStatus(Status.PENDENTE);
        long agendamentosConfirmados = agendamentoRepository.countByStatus(Status.AGENDADO);
        long agendamentosConcluidos = agendamentoRepository.countByStatus(Status.CONCLUIDO);
        long agendamentosCancelados = agendamentoRepository.countByStatus(Status.CANCELADO);

        // Agendamentos de hoje
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioDia = hoje.atStartOfDay();
        LocalDateTime fimDia = hoje.atTime(23, 59, 59);
        long agendamentosHoje = agendamentoRepository.countByDtAgendamentoBetween(inicioDia, fimDia);

        return new AgendamentoEstatisticasDTO(
                totalAgendamentos,
                agendamentosPendentes,
                agendamentosConfirmados,
                agendamentosConcluidos,
                agendamentosCancelados,
                agendamentosHoje
        );
    }

    // Método para reagendar um agendamento
    @Transactional
    public AgendamentoDTO reagendarAgendamento(Long id, LocalDateTime novaDataHora) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento com ID " + id + " não encontrado."));

        // Verificar se o agendamento pode ser reagendado
        if (agendamento.getStatus() == Status.CONCLUIDO || agendamento.getStatus() == Status.CANCELADO) {
            throw new IllegalArgumentException("Não é possível reagendar um agendamento " +
                    agendamento.getStatus().getDescricao().toLowerCase() + ".");
        }

        // Validar nova data
        if (novaDataHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível reagendar para o passado.");
        }

        // Remover bloqueios antigos
        if (agendamento.getBloqueioAgenda() != null) {
            disponibilidadeRepository.delete(agendamento.getBloqueioAgenda());
        }

        // Atualizar data do agendamento
        agendamento.setDtAgendamento(novaDataHora);
        agendamento.setStatus(Status.REAGENDADO);

        // Validar disponibilidade na nova data
        validarDisponibilidadeAgendamento(agendamento);

        // Criar novos bloqueios
        atualizarBloqueiosAgenda(agendamento);

        // Atualizar cobrança se necessário
        atualizarCobrancaAgendamento(agendamento);

        agendamento.setDtAtualizacao(LocalDateTime.now());
        Agendamento agendamentoAtualizado = agendamentoRepository.save(agendamento);

        return new AgendamentoDTO(agendamentoAtualizado);
    }

    public List<AgendamentoDTO> filtrarAgendamentos(AgendamentoFiltroDTO filtro) {
        List<Agendamento> agendamentos = agendamentoRepository.findAll();

        // Aplicar filtros
        if (filtro.getClienteId() != null) {
            Cliente cliente = clienteRepository.findById(filtro.getClienteId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
            agendamentos = agendamentos.stream()
                    .filter(a -> a.getCliente().equals(cliente))
                    .collect(Collectors.toList());
        }

        if (filtro.getNomeCliente() != null && !filtro.getNomeCliente().trim().isEmpty()) {
            String nomeClienteLower = filtro.getNomeCliente().toLowerCase();
            agendamentos = agendamentos.stream()
                    .filter(a -> a.getCliente().getNomeCompleto().toLowerCase().contains(nomeClienteLower))
                    .collect(Collectors.toList());
        }

        if (filtro.getFuncionarioIds() != null && !filtro.getFuncionarioIds().isEmpty()) {
            agendamentos = agendamentos.stream()
                    .filter(a -> a.getFuncionarios().stream()
                            .anyMatch(f -> filtro.getFuncionarioIds().contains(f.getId())))
                    .collect(Collectors.toList());
        }

        if (filtro.getNomeFuncionario() != null && !filtro.getNomeFuncionario().trim().isEmpty()) {
            String nomeFuncionarioLower = filtro.getNomeFuncionario().toLowerCase();
            agendamentos = agendamentos.stream()
                    .filter(a -> a.getFuncionarios().stream()
                            .anyMatch(f -> f.getNomeCompleto().toLowerCase().contains(nomeFuncionarioLower)))
                    .collect(Collectors.toList());
        }

        if (filtro.getStatus() != null && !filtro.getStatus().isEmpty()) {
            List<Status> statusEnum = filtro.getStatus().stream()
                    .map(s -> Status.valueOf(s.toUpperCase()))
                    .collect(Collectors.toList());
            agendamentos = agendamentos.stream()
                    .filter(a -> statusEnum.contains(a.getStatus()))
                    .collect(Collectors.toList());
        }

        if (filtro.getDataInicio() != null) {
            LocalDateTime dataInicioDateTime = filtro.getDataInicio().atStartOfDay();
            agendamentos = agendamentos.stream()
                    .filter(a -> a.getDtAgendamento().isAfter(dataInicioDateTime) ||
                            a.getDtAgendamento().isEqual(dataInicioDateTime))
                    .collect(Collectors.toList());
        }

        if (filtro.getDataFim() != null) {
            LocalDateTime dataFimDateTime = filtro.getDataFim().atTime(23, 59, 59);
            agendamentos = agendamentos.stream()
                    .filter(a -> a.getDtAgendamento().isBefore(dataFimDateTime) ||
                            a.getDtAgendamento().isEqual(dataFimDateTime))
                    .collect(Collectors.toList());
        }

        if (filtro.getServicoIds() != null && !filtro.getServicoIds().isEmpty()) {
            agendamentos = agendamentos.stream()
                    .filter(a -> a.getServicos().stream()
                            .anyMatch(s -> filtro.getServicoIds().contains(s.getId())))
                    .collect(Collectors.toList());
        }

        if (filtro.getNomeServico() != null && !filtro.getNomeServico().trim().isEmpty()) {
            String nomeServicoLower = filtro.getNomeServico().toLowerCase();
            agendamentos = agendamentos.stream()
                    .filter(a -> a.getServicos().stream()
                            .anyMatch(s -> s.getNome().toLowerCase().contains(nomeServicoLower)))
                    .collect(Collectors.toList());
        }

        // Ordenar por data de agendamento
        agendamentos.sort((a1, a2) -> a1.getDtAgendamento().compareTo(a2.getDtAgendamento()));

        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    public List<StatusAgendamentoDTO> getAllStatusDisponiveis() {
        return Arrays.stream(Status.values())
                .map(status -> new StatusAgendamentoDTO(
                        status.name(),
                        status.getDescricao(),
                        getStatusColor(status),
                        isStatusAtivo(status)
                ))
                .collect(Collectors.toList());
    }

    private String getStatusColor(Status status) {
        switch (status) {
            case PENDENTE: return "#FFA500"; // Laranja
            case AGENDADO: return "#007BFF"; // Azul
            case CONFIRMADO: return "#28A745"; // Verde
            case EM_ESPERA: return "#FFC107"; // Amarelo
            case CONCLUIDO: return "#28A745"; // Verde
            case CANCELADO: return "#DC3545"; // Vermelho
            case PAGO: return "#28A745"; // Verde
            case EM_ANDAMENTO: return "#17A2B8"; // Azul claro
            case NAO_COMPARECEU: return "#6C757D"; // Cinza
            case REAGENDADO: return "#007BFF"; // Azul
            case VENCIDA: return "#DC3545"; // Vermelho
            default: return "#6C757D"; // Cinza padrão
        }
    }

    private boolean isStatusAtivo(Status status) {
        return status != Status.CANCELADO && status != Status.CONCLUIDO && status != Status.VENCIDA;
    }



    /**
     * Consulta funcionários que prestam TODOS os serviços informados
     */
    public FuncionarioServicoResponse consultarFuncionariosPorServicos(List<Long> servicoIds) {
        if (servicoIds == null || servicoIds.isEmpty()) {
            throw new IllegalArgumentException("Lista de serviços não pode estar vazia.");
        }

        // Verificar se todos os serviços existem
        List<Servico> servicos = servicoIds.stream()
                .map(id -> servicoRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Serviço com ID " + id + " não encontrado.")))
                .collect(Collectors.toList());

        // Buscar funcionários que prestam TODOS os serviços (excluindo deletados e desabilitados)
        List<Funcionario> funcionarios = funcionarioRepository.findAll().stream()
                .filter(funcionario -> !funcionario.isDeletado() &&
                        funcionario.isAtivo() &&
                        funcionario.getServicos() != null &&
                        funcionario.getServicos().containsAll(servicos))
                .collect(Collectors.toList());

        // Converter para DTOs
        List<FuncionarioServicoResponse.FuncionarioResumoDTO> funcionariosDTO = funcionarios.stream()
                .map(FuncionarioServicoResponse.FuncionarioResumoDTO::new)
                .collect(Collectors.toList());

//        List<FuncionarioServicoResponse.ServicoResumoDTO> servicosDTO = servicos.stream()
//                .map(FuncionarioServicoResponse.ServicoResumoDTO::new)
//                .collect(Collectors.toList());

        return new FuncionarioServicoResponse(funcionariosDTO, "POR_SERVICOS");
    }

    /**
     * Consulta serviços que TODOS os funcionários informados prestam em comum
     */
    public FuncionarioServicoResponse consultarServicosPorFuncionarios(List<Long> funcionarioIds) {
        if (funcionarioIds == null || funcionarioIds.isEmpty()) {
            throw new IllegalArgumentException("Lista de funcionários não pode estar vazia.");
        }

        // Verificar se todos os funcionários existem
        List<Funcionario> funcionarios = funcionarioIds.stream()
                .map(id -> funcionarioRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado.")))
                .collect(Collectors.toList());

        // Buscar serviços que TODOS os funcionários prestam
        List<Servico> servicosComuns = new ArrayList<>();

        if (!funcionarios.isEmpty()) {
            // Começar com os serviços do primeiro funcionário
            List<Servico> servicosIntersecao = new ArrayList<>();
            if (funcionarios.get(0).getServicos() != null) {
                servicosIntersecao.addAll(funcionarios.get(0).getServicos());
            }

            // Para cada funcionário seguinte, manter apenas os serviços em comum
            for (int i = 1; i < funcionarios.size(); i++) {
                Funcionario funcionario = funcionarios.get(i);
                if (funcionario.getServicos() != null) {
                    servicosIntersecao.retainAll(funcionario.getServicos());
                } else {
                    servicosIntersecao.clear(); // Se algum funcionário não tem serviços, não há interseção
                    break;
                }
            }

            servicosComuns = servicosIntersecao;
        }

        // Converter para DTOs
//        List<FuncionarioServicoResponse.FuncionarioResumoDTO> funcionariosDTO = funcionarios.stream()
//                .map(FuncionarioServicoResponse.FuncionarioResumoDTO::new)
//                .collect(Collectors.toList());

        List<FuncionarioServicoResponse.ServicoResumoDTO> servicosDTO = servicosComuns.stream()
                .map(FuncionarioServicoResponse.ServicoResumoDTO::new)
                .collect(Collectors.toList());

        return new FuncionarioServicoResponse(servicosDTO, "POR_FUNCIONARIOS");
    }

    /**
     * Método unificado que decide qual consulta fazer baseado nos parâmetros
     */
    public FuncionarioServicoResponse consultarRelacionamentos(ConsultaRelacionamentoRequest request) {
        boolean temServicos = request.getServicoIds() != null && !request.getServicoIds().isEmpty();
        boolean temFuncionarios = request.getFuncionarioIds() != null && !request.getFuncionarioIds().isEmpty();

        if (temServicos && temFuncionarios) {
            throw new IllegalArgumentException("Informe apenas serviços OU funcionários, não ambos.");
        }

        if (!temServicos && !temFuncionarios) {
            throw new IllegalArgumentException("Informe pelo menos uma lista de serviços ou funcionários.");
        }

        if (temServicos) {
            return consultarFuncionariosPorServicos(request.getServicoIds());
        } else {
            return consultarServicosPorFuncionarios(request.getFuncionarioIds());
        }
    }




//        // Método para marcar um agendamento como concluído, que também pode liberar o bloqueio se necessário
//    @Transactional
//    public Agendamento concluirAgendamento(Long agendamentoId) {
//        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
//                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));
//
//        agendamento.marcarComoConcluido(); // Chama o método da entidade Agendamento
//        // Opcional: remover ou inativar o BloqueioAgenda se o agendamento for concluído
//        // Se a ideia for manter o histórico dos bloqueios, você pode apenas atualizar o status do agendamento.
//
//        return agendamentoRepository.save(agendamento);
//    }
//
//    // Novo método para validar a disponibilidade (usado internamente e para garantir)
//    private void validarDisponibilidade(Funcionario funcionario, LocalDateTime inicioAgendamento, LocalDateTime fimAgendamento, int duracaoTotalMinutos) {
//        DayOfWeek diaSemanaAgendamento = inicioAgendamento.getDayOfWeek();
//        LocalTime horaInicioAgendamento = inicioAgendamento.toLocalTime();
//        LocalTime horaFimAgendamento = fimAgendamento.toLocalTime();
//
//        // 1. Verificar a Jornada de Trabalho
//        JornadaTrabalho jornada = jornadaTrabalhoRepository.findByFuncionarioAndDiaSemana(
//                        funcionario, DiaSemana.valueOf(diaSemanaAgendamento.name()))
//                .orElseThrow(() -> new RuntimeException("Funcionário não tem jornada de trabalho definida para este dia."));
//
//        if (horaInicioAgendamento.isBefore(jornada.getHoraInicio()) ||
//                horaFimAgendamento.isAfter(jornada.getHoraFim())) {
//            throw new IllegalArgumentException("Funcionário " + funcionario.getNomeCompleto() + " não está disponível neste horário de acordo com sua jornada.");
//        }
//
//        // 2. Verificar Conflitos de Bloqueio
//        List<BloqueioAgenda> bloqueiosConflitantes = disponibilidadeRepository.findByFuncionarioAndInicioBloqueioBetween(
//                funcionario, inicioAgendamento.toLocalDate().atStartOfDay(), inicioAgendamento.toLocalDate().plusDays(1).atStartOfDay());
//
//        boolean temConflito = bloqueiosConflitantes.stream().anyMatch(bloqueio ->
//                (inicioAgendamento.isBefore(bloqueio.getFimBloqueio()) && fimAgendamento.isAfter(bloqueio.getInicioBloqueio()))
//        );
//
//        if (temConflito) {
//            throw new IllegalArgumentException("Funcionário " + funcionario.getNomeCompleto() + " já possui um bloqueio na agenda neste período.");
//        }
//    }
//
//
//    // --- NOVO MÉTODO PARA OBTER DISPONIBILIDADE ---
//    public List<HorarioDisponivelResponse> getHorariosDisponiveis(DisponibilidadeRequest request) {
//        // 1. Recuperar o Funcionário
//        Funcionario funcionario = funcionarioRepository.findById(request.getFuncionarioId())
//                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado."));
//
//        // 2. Calcular a Duração Total dos Serviços + Tolerância
//        int duracaoServicos = request.getServicoIds().stream()
//                .map(servicoRepository::findById)
//                .filter(Optional::isPresent)
//                .map(Optional::get)
//                .mapToInt(Servico::getTempoEstimadoMinutos)
//                .sum();
//
//        int duracaoTotalNecessaria = duracaoServicos + config.getConfigAgendamento().getToleranciaAgendamento(); // Adiciona a tolerância
//
//        // 3. Obter a Jornada de Trabalho para o dia desejado
//        DayOfWeek diaDaSemana = request.getDataDesejada().getDayOfWeek();
//        JornadaTrabalho jornada = jornadaTrabalhoRepository.findByFuncionarioAndDiaSemana(
//                        funcionario, DiaSemana.fromDayOfWeek(diaDaSemana)) // <--- Use o novo método de conversão
//                .orElse(null); // Retorna null se não houver jornada (funcionário não trabalha nesse dia)
//
//        if (jornada == null) {
//            return new ArrayList<>(); // Nenhum horário disponível se não houver jornada
//        }
//
//        // 4. Obter todos os bloqueios (agendamentos, almoços, etc.) para o funcionário no dia
//        LocalDateTime inicioDoDia = request.getDataDesejada().atStartOfDay();
//        LocalDateTime fimDoDia = request.getDataDesejada().plusDays(1).atStartOfDay().minusNanos(1); // Fim do dia antes da meia-noite
//
//        List<BloqueioAgenda> bloqueios = disponibilidadeRepository.findByFuncionarioAndInicioBloqueioBetween(
//                funcionario, inicioDoDia, fimDoDia);
//
//        // Adiciona a própria jornada de trabalho como um "bloqueio" inverso para facilitar o cálculo dos slots
//        // Tudo fora da jornada é considerado "bloqueado"
//        List<BloqueioAgenda> todosBloqueiosOrdenados = new ArrayList<>(bloqueios);
//
//        // Garante que o início da jornada seja um "bloqueio" do dia anterior e o fim um "bloqueio" para o dia seguinte
//        // Isso ajuda a delimitar os slots dentro da jornada
//        todosBloqueiosOrdenados.add(new BloqueioAgenda(
//                funcionario,
//                request.getDataDesejada().atStartOfDay(),
//                request.getDataDesejada().atTime(jornada.getHoraInicio()),
//                "Fora da Jornada (Antes)",
//                TipoBloqueio.OUTRO, null
//        ));
//        todosBloqueiosOrdenados.add(new BloqueioAgenda(
//                funcionario,
//                request.getDataDesejada().atTime(jornada.getHoraFim()),
//                request.getDataDesejada().plusDays(1).atStartOfDay(),
//                "Fora da Jornada (Depois)",
//                TipoBloqueio.OUTRO, null
//        ));
//
//
//        // Ordenar os bloqueios por hora de início
//        todosBloqueiosOrdenados.sort(Comparator.comparing(BloqueioAgenda::getInicioBloqueio));
//
//        List<HorarioDisponivelResponse> horariosDisponiveis = new ArrayList<>();
//        LocalDateTime ponteiroAtual = request.getDataDesejada().atTime(jornada.getHoraInicio());
//
//        for (BloqueioAgenda bloqueio : todosBloqueiosOrdenados) {
//            LocalDateTime inicioBloqueioAtual = bloqueio.getInicioBloqueio();
//            LocalDateTime fimBloqueioAtual = bloqueio.getFimBloqueio();
//
//            // Ajusta o início do bloqueio e fim do bloqueio para o dia desejado, caso o bloqueio se estenda por dias
//            inicioBloqueioAtual = inicioBloqueioAtual.isBefore(inicioDoDia) ? inicioDoDia : inicioBloqueioAtual;
//            fimBloqueioAtual = fimBloqueioAtual.isAfter(fimDoDia) ? fimDoDia : fimBloqueioAtual;
//
//
//            // Se houver um gap entre o ponteiro atual e o início do próximo bloqueio
//            if (ponteiroAtual.isBefore(inicioBloqueioAtual)) {
//                // Calcular a duração do gap
//                long duracaoGapMinutos = java.time.Duration.between(ponteiroAtual, inicioBloqueioAtual).toMinutes();
//
//                // Enquanto o gap for maior ou igual ao tempo necessário para o serviço + tolerância
//                while (duracaoGapMinutos >= duracaoTotalNecessaria) {
//                    LocalDateTime slotFim = ponteiroAtual.plusMinutes(duracaoServicos); // Fim do slot sem tolerância
//                    // Garante que o slot não ultrapasse o início do bloqueio atual
//                    if (slotFim.isAfter(inicioBloqueioAtual)) {
//                        break; // Não cabe o serviço antes do bloqueio
//                    }
//
//                    // Se o slot estiver dentro da jornada de trabalho (já garantido pelos "bloqueios" de jornada)
//                    // E se o slot não ultrapassar a meia-noite (para evitar agendamentos em dias diferentes)
//                    if (slotFim.toLocalDate().isEqual(request.getDataDesejada())) {
//                        horariosDisponiveis.add(new HorarioDisponivelResponse(
//                                ponteiroAtual.toLocalTime(),
//                                slotFim.toLocalTime(),
//                                ponteiroAtual.toLocalTime() + " - " + slotFim.toLocalTime()
//                        ));
//                    }
//
//
//                    // Avança o ponteiro para o próximo slot possível (fim do serviço + tolerância)
//                    ponteiroAtual = slotFim.plusMinutes(config.getConfigAgendamento().getToleranciaAgendamento());
//                    duracaoGapMinutos = java.time.Duration.between(ponteiroAtual, inicioBloqueioAtual).toMinutes();
//                }
//            }
//
//            // Atualiza o ponteiro para o fim do bloqueio atual, garantindo que não retroceda
//            ponteiroAtual = ponteiroAtual.isAfter(fimBloqueioAtual) ? ponteiroAtual : fimBloqueioAtual;
//        }
//
//        // Caso haja espaço disponível após o último bloqueio até o fim da jornada
//        LocalDateTime fimDaJornada = request.getDataDesejada().atTime(jornada.getHoraFim());
//        if (ponteiroAtual.isBefore(fimDaJornada)) {
//            long duracaoFinalGapMinutos = java.time.Duration.between(ponteiroAtual, fimDaJornada).toMinutes();
//            while (duracaoFinalGapMinutos >= duracaoTotalNecessaria) {
//                LocalDateTime slotFim = ponteiroAtual.plusMinutes(duracaoServicos);
//                if (slotFim.isAfter(fimDaJornada)) {
//                    break; // Não cabe o serviço antes do fim da jornada
//                }
//                horariosDisponiveis.add(new HorarioDisponivelResponse(
//                        ponteiroAtual.toLocalTime(),
//                        slotFim.toLocalTime(),
//                        ponteiroAtual.toLocalTime() + " - " + slotFim.toLocalTime()
//                ));
//                ponteiroAtual = slotFim.plusMinutes(config.getConfigAgendamento().getToleranciaAgendamento());
//                duracaoFinalGapMinutos = java.time.Duration.between(ponteiroAtual, fimDaJornada).toMinutes();
//            }
//        }
//
//
//        // Garante que os horários estejam dentro da jornada (caso os bloqueios não cubram a totalidade)
//        LocalTime inicioJornada = jornada.getHoraInicio();
//        LocalTime fimJornada = jornada.getHoraFim();
//
//        return horariosDisponiveis.stream()
//                .filter(horario -> !horario.getHoraInicio().isBefore(inicioJornada) && !horario.getHoraFim().isAfter(fimJornada))
//                .sorted(Comparator.comparing(HorarioDisponivelResponse::getHoraInicio)) // Garante ordem
//                .collect(Collectors.toList());
//    }

}
