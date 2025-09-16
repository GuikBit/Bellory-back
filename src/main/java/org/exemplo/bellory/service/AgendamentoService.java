package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.model.dto.*;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.funcionario.DisponibilidadeRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.funcionario.JornadaTrabalhoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.stereotype.Service;

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

    private final TransacaoService transacaoService;


    private static final int TOLERANCIA_MINUTOS = 10;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              DisponibilidadeRepository disponibilidadeRepository,
                              JornadaTrabalhoRepository jornadaTrabalhoRepository,
                              FuncionarioRepository funcionarioRepository,
                              ServicoRepository servicoRepository,
                              ClienteRepository clienteRepository,
                              OrganizacaoRepository organizacaoRepository,
                              CobrancaRepository cobrancaRepository, TransacaoService transacaoService) {
        this.agendamentoRepository = agendamentoRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.jornadaTrabalhoRepository = jornadaTrabalhoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.servicoRepository = servicoRepository;
        this.clienteRepository = clienteRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.cobrancaRepository = cobrancaRepository;
        this.transacaoService = transacaoService;
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

    @Transactional
    public AgendamentoDTO createAgendamentoCompleto(AgendamentoCreateDTO dto) {
        // 1. Buscar entidades relacionadas
        Organizacao organizacao = organizacaoRepository.findById(dto.getOrganizacaoId())
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        List<Servico> servicos = dto.getServicoIds().stream()
                .map(id -> servicoRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Serviço com ID " + id + " não encontrado.")))
                .collect(Collectors.toList());

        List<Funcionario> funcionarios = dto.getFuncionarioIds().stream()
                .map(id -> funcionarioRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado.")))
                .collect(Collectors.toList());

        // 2. Criar agendamento
        Agendamento agendamento = new Agendamento();
        agendamento.setOrganizacao(organizacao);
        agendamento.setCliente(cliente);
        agendamento.setServicos(servicos);
        agendamento.setFuncionarios(funcionarios);
        agendamento.setDtAgendamento(dto.getDtAgendamento());
        agendamento.setObservacao(dto.getObservacao());
        agendamento.setStatus(Status.PENDENTE);
        agendamento.setDtCriacao(LocalDateTime.now());

        // 3. Validar e salvar agendamento
        Agendamento agendamentoSalvo = criarAgendamento(agendamento);

        // 4. Criar cobrança através do TransactionService
        Cobranca cobranca = transacaoService.criarCobrancaParaAgendamento(agendamentoSalvo);

        // 5. Estabelecer relacionamento bidirecional
        agendamentoSalvo.setCobranca(cobranca);
        agendamentoRepository.save(agendamentoSalvo);

        return new AgendamentoDTO(agendamentoSalvo);
    }


    public List<AgendamentoDTO> getAllAgendamentos() {
        List<Agendamento> agendamentos = agendamentoRepository.findAll();
        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    public AgendamentoDTO getAgendamentoById(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento com ID " + id + " não encontrado."));
        return new AgendamentoDTO(agendamento);
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
        if (agendamento.getCobranca() != null &&
                agendamento.getCobranca().getStatusCobranca() != Cobranca.StatusCobranca.PAGO) {

            agendamento.getCobranca().cancelar();
            cobrancaRepository.save(agendamento.getCobranca());
        }

        agendamentoRepository.save(agendamento);
    }

    @Transactional
    public AgendamentoDTO updateStatusAgendamento(Long id, String status) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento com ID " + id + " não encontrado."));

        try {
            Status novoStatus = Status.valueOf(status.toUpperCase());
            alterarStatusAgendamento(agendamento, novoStatus);
            agendamento.setDtAtualizacao(LocalDateTime.now());

            Agendamento agendamentoAtualizado = agendamentoRepository.save(agendamento);
            return new AgendamentoDTO(agendamentoAtualizado);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: " + status);
        }
    }

    private void validarDisponibilidadeAgendamento(Agendamento agendamento) {
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
        if (agendamento.getCobranca() != null) {
            Cobranca cobranca = agendamento.getCobranca();

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
            case AGENDADO:
                agendamento.marcarComoAgendado();
                break;

            case CONFIRMADO:
                agendamento.setStatus(Status.CONFIRMADO);
                // Enviar notificação de confirmação se necessário
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
                // Cancelar cobrança somente se não estiver paga
                if (agendamento.getCobranca() != null &&
                        !agendamento.getCobranca().isPaga()) {
                    agendamento.getCobranca().cancelar();
                    cobrancaRepository.save(agendamento.getCobranca());
                }
                break;

            case EM_ESPERA:
                agendamento.colocarEmEspera();
                break;

            case NAO_COMPARECEU:
                agendamento.setStatus(Status.NAO_COMPARECEU);
                // Aplicar taxa de não comparecimento se configurada
                aplicarTaxaNaoComparecimento(agendamento);
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


    @Transactional
    public Agendamento criarAgendamento(Agendamento novoAgendamento) {
        // 1. Validar a Data e Hora do Agendamento
        if (novoAgendamento.getDtAgendamento().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível agendar para o passado.");
        }

        // 2. Calcular a Duração Total dos Serviços
        int duracaoTotalMinutos = novoAgendamento.getServicos().stream()
                .mapToInt(Servico::getTempoEstimadoMinutos)
                .sum();

        // 3. Definir o Horário de Término do Agendamento
        LocalDateTime dataHoraFimAgendamento = novoAgendamento.getDtAgendamento().plusMinutes(duracaoTotalMinutos);

        // 4. Para cada funcionário envolvido no agendamento:
        for (Funcionario funcionario : novoAgendamento.getFuncionarios()) {
            // a. Verificar a Jornada de Trabalho do Funcionário
            DayOfWeek diaSemanaAgendamento = novoAgendamento.getDtAgendamento().getDayOfWeek();
            LocalTime horaInicioAgendamento = novoAgendamento.getDtAgendamento().toLocalTime();
            LocalTime horaFimAgendamento = dataHoraFimAgendamento.toLocalTime();

            // Encontre a jornada de trabalho para o dia específico
            JornadaTrabalho jornada = jornadaTrabalhoRepository.findByFuncionarioAndDiaSemana(
                    funcionario,
                    DiaSemana.fromDayOfWeek(diaSemanaAgendamento) // <- Use o método de conversão
            ).orElseThrow(() ->
                    new RuntimeException("Funcionário não tem jornada de trabalho definida para este dia.")
            );

            // Verifica se o agendamento está dentro da jornada
            if (horaInicioAgendamento.isBefore(jornada.getHoraInicio()) ||
                    horaFimAgendamento.isAfter(jornada.getHoraFim())) {
                throw new IllegalArgumentException("Funcionário " + funcionario.getNomeCompleto() + " não está disponível neste horário de acordo com sua jornada.");
            }

            // b. Verificar Conflitos de Bloqueio na Agenda do Funcionário
            List<BloqueioAgenda> bloqueiosConflitantes = disponibilidadeRepository.findByFuncionarioAndInicioBloqueioBetween(
                    funcionario,
                    novoAgendamento.getDtAgendamento(),
                    dataHoraFimAgendamento
            );

            // Filtra bloqueios que realmente se sobrepõem
            boolean temConflito = bloqueiosConflitantes.stream().anyMatch(bloqueio ->
                    (novoAgendamento.getDtAgendamento().isBefore(bloqueio.getFimBloqueio()) &&
                            dataHoraFimAgendamento.isAfter(bloqueio.getInicioBloqueio()))
            );

            if (temConflito) {
                throw new IllegalArgumentException("Funcionário " + funcionario.getNomeCompleto() + " já possui um bloqueio na agenda neste período.");
            }
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

    private void validarDisponibilidade(Funcionario funcionario, LocalDateTime inicioAgendamento, LocalDateTime fimAgendamento, int duracaoTotalMinutos) {
        DayOfWeek diaSemanaAgendamento = inicioAgendamento.getDayOfWeek();
        LocalTime horaInicioAgendamento = inicioAgendamento.toLocalTime();
        LocalTime horaFimAgendamento = fimAgendamento.toLocalTime();

        // 1. Verificar a Jornada de Trabalho
        JornadaTrabalho jornada = jornadaTrabalhoRepository.findByFuncionarioAndDiaSemana(
                funcionario,
                DiaSemana.fromDayOfWeek(diaSemanaAgendamento) // <- Use o método de conversão aqui também
        ).orElseThrow(() -> new RuntimeException("Funcionário não tem jornada de trabalho definida para este dia."));

        if (horaInicioAgendamento.isBefore(jornada.getHoraInicio()) ||
                horaFimAgendamento.isAfter(jornada.getHoraFim())) {
            throw new IllegalArgumentException("Funcionário " + funcionario.getNomeCompleto() + " não está disponível neste horário de acordo com sua jornada.");
        }

        // 2. Verificar Conflitos de Bloqueio
        List<BloqueioAgenda> bloqueiosConflitantes = disponibilidadeRepository.findByFuncionarioAndInicioBloqueioBetween(
                funcionario, inicioAgendamento.toLocalDate().atStartOfDay(), inicioAgendamento.toLocalDate().plusDays(1).atStartOfDay());

        boolean temConflito = bloqueiosConflitantes.stream().anyMatch(bloqueio ->
                (inicioAgendamento.isBefore(bloqueio.getFimBloqueio()) && fimAgendamento.isAfter(bloqueio.getInicioBloqueio()))
        );

        if (temConflito) {
            throw new IllegalArgumentException("Funcionário " + funcionario.getNomeCompleto() + " já possui um bloqueio na agenda neste período.");
        }
    }

    public List<HorarioDisponivelResponse> getHorariosDisponiveis(DisponibilidadeRequest request) {
        // 1. Recuperar o Funcionário
        Funcionario funcionario = funcionarioRepository.findById(request.getFuncionarioId())
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado."));

        // 2. Calcular a Duração Total dos Serviços + Tolerância
        int duracaoServicos = request.getServicoIds().stream()
                .map(servicoRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .mapToInt(Servico::getTempoEstimadoMinutos)
                .sum();

        int duracaoTotalNecessaria = duracaoServicos + TOLERANCIA_MINUTOS;

        // 3. Obter a Jornada de Trabalho para o dia desejado
        DayOfWeek diaDaSemana = request.getDataDesejada().getDayOfWeek();
        JornadaTrabalho jornada = jornadaTrabalhoRepository.findByFuncionarioAndDiaSemana(
                        funcionario, DiaSemana.fromDayOfWeek(diaDaSemana))
                .orElse(null);

        if (jornada == null) {
            return new ArrayList<>();
        }

        // 4. Obter todos os bloqueios para o funcionário no dia
        LocalDateTime inicioDoDia = request.getDataDesejada().atStartOfDay();
        LocalDateTime fimDoDia = request.getDataDesejada().plusDays(1).atStartOfDay().minusNanos(1);

        List<BloqueioAgenda> bloqueios = disponibilidadeRepository.findByFuncionarioAndInicioBloqueioBetween(
                funcionario, inicioDoDia, fimDoDia);

        List<BloqueioAgenda> todosBloqueiosOrdenados = new ArrayList<>(bloqueios);

        todosBloqueiosOrdenados.add(new BloqueioAgenda(
                funcionario,
                request.getDataDesejada().atStartOfDay(),
                request.getDataDesejada().atTime(jornada.getHoraInicio()),
                "Fora da Jornada (Antes)",
                TipoBloqueio.OUTRO, null
        ));
        todosBloqueiosOrdenados.add(new BloqueioAgenda(
                funcionario,
                request.getDataDesejada().atTime(jornada.getHoraFim()),
                request.getDataDesejada().plusDays(1).atStartOfDay(),
                "Fora da Jornada (Depois)",
                TipoBloqueio.OUTRO, null
        ));

        todosBloqueiosOrdenados.sort(Comparator.comparing(BloqueioAgenda::getInicioBloqueio));

        List<HorarioDisponivelResponse> horariosDisponiveis = new ArrayList<>();
        LocalDateTime ponteiroAtual = request.getDataDesejada().atTime(jornada.getHoraInicio());

        for (BloqueioAgenda bloqueio : todosBloqueiosOrdenados) {
            LocalDateTime inicioBloqueioAtual = bloqueio.getInicioBloqueio();
            LocalDateTime fimBloqueioAtual = bloqueio.getFimBloqueio();

            inicioBloqueioAtual = inicioBloqueioAtual.isBefore(inicioDoDia) ? inicioDoDia : inicioBloqueioAtual;
            fimBloqueioAtual = fimBloqueioAtual.isAfter(fimDoDia) ? fimDoDia : fimBloqueioAtual;

            if (ponteiroAtual.isBefore(inicioBloqueioAtual)) {
                long duracaoGapMinutos = java.time.Duration.between(ponteiroAtual, inicioBloqueioAtual).toMinutes();

                while (duracaoGapMinutos >= duracaoTotalNecessaria) {
                    LocalDateTime slotFim = ponteiroAtual.plusMinutes(duracaoServicos);
                    if (slotFim.isAfter(inicioBloqueioAtual)) {
                        break;
                    }

                    if (slotFim.toLocalDate().isEqual(request.getDataDesejada())) {
                        horariosDisponiveis.add(new HorarioDisponivelResponse(
                                ponteiroAtual.toLocalTime(),
                                slotFim.toLocalTime(),
                                ponteiroAtual.toLocalTime() + " - " + slotFim.toLocalTime()
                        ));
                    }

                    ponteiroAtual = slotFim.plusMinutes(TOLERANCIA_MINUTOS);
                    duracaoGapMinutos = java.time.Duration.between(ponteiroAtual, inicioBloqueioAtual).toMinutes();
                }
            }

            ponteiroAtual = ponteiroAtual.isAfter(fimBloqueioAtual) ? ponteiroAtual : fimBloqueioAtual;
        }

        LocalDateTime fimDaJornada = request.getDataDesejada().atTime(jornada.getHoraFim());
        if (ponteiroAtual.isBefore(fimDaJornada)) {
            long duracaoFinalGapMinutos = java.time.Duration.between(ponteiroAtual, fimDaJornada).toMinutes();
            while (duracaoFinalGapMinutos >= duracaoTotalNecessaria) {
                LocalDateTime slotFim = ponteiroAtual.plusMinutes(duracaoServicos);
                if (slotFim.isAfter(fimDaJornada)) {
                    break;
                }
                horariosDisponiveis.add(new HorarioDisponivelResponse(
                        ponteiroAtual.toLocalTime(),
                        slotFim.toLocalTime(),
                        ponteiroAtual.toLocalTime() + " - " + slotFim.toLocalTime()
                ));
                ponteiroAtual = slotFim.plusMinutes(TOLERANCIA_MINUTOS);
                duracaoFinalGapMinutos = java.time.Duration.between(ponteiroAtual, fimDaJornada).toMinutes();
            }
        }

        LocalTime inicioJornada = jornada.getHoraInicio();
        LocalTime fimJornada = jornada.getHoraFim();

        return horariosDisponiveis.stream()
                .filter(horario -> !horario.getHoraInicio().isBefore(inicioJornada) && !horario.getHoraFim().isAfter(fimJornada))
                .sorted(Comparator.comparing(HorarioDisponivelResponse::getHoraInicio))
                .collect(Collectors.toList());
    }

    public List<AgendamentoDTO> getAgendamentosComCobrancasPendentes() {
        List<Agendamento> agendamentos = agendamentoRepository.findAll().stream()
                .filter(a -> a.getCobranca() != null && a.getCobranca().isPendente())
                .sorted((a1, a2) -> a1.getDtAgendamento().compareTo(a2.getDtAgendamento()))
                .collect(Collectors.toList());

        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    public List<AgendamentoDTO> getAgendamentosVencidos() {
        LocalDate hoje = LocalDate.now();
        List<Agendamento> agendamentos = agendamentoRepository.findAll().stream()
                .filter(a -> a.getCobranca() != null &&
                        a.getCobranca().getDtVencimento() != null &&
                        a.getCobranca().getDtVencimento().isBefore(hoje) &&
                        a.getCobranca().isPendente())
                .sorted((a1, a2) -> a1.getCobranca().getDtVencimento().compareTo(a2.getCobranca().getDtVencimento()))
                .collect(Collectors.toList());

        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    public List<AgendamentoDTO> getAgendamentosByCliente(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente com ID " + clienteId + " não encontrado."));

        List<Agendamento> agendamentos = agendamentoRepository.findByCliente(cliente);
        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    // Método para buscar agendamentos por funcionário
    public List<AgendamentoDTO> getAgendamentosByFuncionario(Long funcionarioId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + funcionarioId + " não encontrado."));

        List<Agendamento> agendamentos = agendamentoRepository.findByFuncionariosContaining(funcionario);

        return agendamentos.stream()
                .map(AgendamentoDTO::new)
                .collect(Collectors.toList());
    }

    // Método para buscar agendamentos por data
    public List<AgendamentoDTO> getAgendamentosByData(LocalDate data) {
        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(23, 59, 59);

        List<Agendamento> agendamentos = agendamentoRepository.findByDtAgendamentoBetween(inicioDia, fimDia);
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
        LocalDate hoje = LocalDate.now();
        return getAgendamentosByData(hoje);
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
//        int duracaoTotalNecessaria = duracaoServicos + TOLERANCIA_MINUTOS; // Adiciona a tolerância
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
//                    ponteiroAtual = slotFim.plusMinutes(TOLERANCIA_MINUTOS);
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
//                ponteiroAtual = slotFim.plusMinutes(TOLERANCIA_MINUTOS);
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
