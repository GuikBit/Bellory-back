package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.model.dto.DisponibilidadeRequest;
import org.exemplo.bellory.model.dto.HorarioDisponivelResponse;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.funcionario.DisponibilidadeRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.funcionario.JornadaTrabalhoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final JornadaTrabalhoRepository jornadaTrabalhoRepository;
    private final FuncionarioRepository funcionarioRepository; // Injetar
    private final ServicoRepository servicoRepository;

    public AgendamentoService(AgendamentoRepository agendamentoRepository, DisponibilidadeRepository disponibilidadeRepository, JornadaTrabalhoRepository jornadaTrabalhoRepository, FuncionarioRepository funcionarioRepository, ServicoRepository servicoRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.jornadaTrabalhoRepository = jornadaTrabalhoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.servicoRepository = servicoRepository;
    }

    private static final int TOLERANCIA_MINUTOS = 10;

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
                    DiaSemana.valueOf(diaSemanaAgendamento.name()) // Segundo parâmetro
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

    // Método para marcar um agendamento como concluído, que também pode liberar o bloqueio se necessário
    @Transactional
    public Agendamento concluirAgendamento(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));

        agendamento.marcarComoConcluido(); // Chama o método da entidade Agendamento
        // Opcional: remover ou inativar o BloqueioAgenda se o agendamento for concluído
        // Se a ideia for manter o histórico dos bloqueios, você pode apenas atualizar o status do agendamento.

        return agendamentoRepository.save(agendamento);
    }

    // Novo método para validar a disponibilidade (usado internamente e para garantir)
    private void validarDisponibilidade(Funcionario funcionario, LocalDateTime inicioAgendamento, LocalDateTime fimAgendamento, int duracaoTotalMinutos) {
        DayOfWeek diaSemanaAgendamento = inicioAgendamento.getDayOfWeek();
        LocalTime horaInicioAgendamento = inicioAgendamento.toLocalTime();
        LocalTime horaFimAgendamento = fimAgendamento.toLocalTime();

        // 1. Verificar a Jornada de Trabalho
        JornadaTrabalho jornada = jornadaTrabalhoRepository.findByFuncionarioAndDiaSemana(
                        funcionario, DiaSemana.valueOf(diaSemanaAgendamento.name()))
                .orElseThrow(() -> new RuntimeException("Funcionário não tem jornada de trabalho definida para este dia."));

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


    // --- NOVO MÉTODO PARA OBTER DISPONIBILIDADE ---
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

        int duracaoTotalNecessaria = duracaoServicos + TOLERANCIA_MINUTOS; // Adiciona a tolerância

        // 3. Obter a Jornada de Trabalho para o dia desejado
        DayOfWeek diaDaSemana = request.getDataDesejada().getDayOfWeek();
        JornadaTrabalho jornada = jornadaTrabalhoRepository.findByFuncionarioAndDiaSemana(
                        funcionario, DiaSemana.fromDayOfWeek(diaDaSemana)) // <--- Use o novo método de conversão
                .orElse(null); // Retorna null se não houver jornada (funcionário não trabalha nesse dia)

        if (jornada == null) {
            return new ArrayList<>(); // Nenhum horário disponível se não houver jornada
        }

        // 4. Obter todos os bloqueios (agendamentos, almoços, etc.) para o funcionário no dia
        LocalDateTime inicioDoDia = request.getDataDesejada().atStartOfDay();
        LocalDateTime fimDoDia = request.getDataDesejada().plusDays(1).atStartOfDay().minusNanos(1); // Fim do dia antes da meia-noite

        List<BloqueioAgenda> bloqueios = disponibilidadeRepository.findByFuncionarioAndInicioBloqueioBetween(
                funcionario, inicioDoDia, fimDoDia);

        // Adiciona a própria jornada de trabalho como um "bloqueio" inverso para facilitar o cálculo dos slots
        // Tudo fora da jornada é considerado "bloqueado"
        List<BloqueioAgenda> todosBloqueiosOrdenados = new ArrayList<>(bloqueios);

        // Garante que o início da jornada seja um "bloqueio" do dia anterior e o fim um "bloqueio" para o dia seguinte
        // Isso ajuda a delimitar os slots dentro da jornada
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


        // Ordenar os bloqueios por hora de início
        todosBloqueiosOrdenados.sort(Comparator.comparing(BloqueioAgenda::getInicioBloqueio));

        List<HorarioDisponivelResponse> horariosDisponiveis = new ArrayList<>();
        LocalDateTime ponteiroAtual = request.getDataDesejada().atTime(jornada.getHoraInicio());

        for (BloqueioAgenda bloqueio : todosBloqueiosOrdenados) {
            LocalDateTime inicioBloqueioAtual = bloqueio.getInicioBloqueio();
            LocalDateTime fimBloqueioAtual = bloqueio.getFimBloqueio();

            // Ajusta o início do bloqueio e fim do bloqueio para o dia desejado, caso o bloqueio se estenda por dias
            inicioBloqueioAtual = inicioBloqueioAtual.isBefore(inicioDoDia) ? inicioDoDia : inicioBloqueioAtual;
            fimBloqueioAtual = fimBloqueioAtual.isAfter(fimDoDia) ? fimDoDia : fimBloqueioAtual;


            // Se houver um gap entre o ponteiro atual e o início do próximo bloqueio
            if (ponteiroAtual.isBefore(inicioBloqueioAtual)) {
                // Calcular a duração do gap
                long duracaoGapMinutos = java.time.Duration.between(ponteiroAtual, inicioBloqueioAtual).toMinutes();

                // Enquanto o gap for maior ou igual ao tempo necessário para o serviço + tolerância
                while (duracaoGapMinutos >= duracaoTotalNecessaria) {
                    LocalDateTime slotFim = ponteiroAtual.plusMinutes(duracaoServicos); // Fim do slot sem tolerância
                    // Garante que o slot não ultrapasse o início do bloqueio atual
                    if (slotFim.isAfter(inicioBloqueioAtual)) {
                        break; // Não cabe o serviço antes do bloqueio
                    }

                    // Se o slot estiver dentro da jornada de trabalho (já garantido pelos "bloqueios" de jornada)
                    // E se o slot não ultrapassar a meia-noite (para evitar agendamentos em dias diferentes)
                    if (slotFim.toLocalDate().isEqual(request.getDataDesejada())) {
                        horariosDisponiveis.add(new HorarioDisponivelResponse(
                                ponteiroAtual.toLocalTime(),
                                slotFim.toLocalTime(),
                                ponteiroAtual.toLocalTime() + " - " + slotFim.toLocalTime()
                        ));
                    }


                    // Avança o ponteiro para o próximo slot possível (fim do serviço + tolerância)
                    ponteiroAtual = slotFim.plusMinutes(TOLERANCIA_MINUTOS);
                    duracaoGapMinutos = java.time.Duration.between(ponteiroAtual, inicioBloqueioAtual).toMinutes();
                }
            }

            // Atualiza o ponteiro para o fim do bloqueio atual, garantindo que não retroceda
            ponteiroAtual = ponteiroAtual.isAfter(fimBloqueioAtual) ? ponteiroAtual : fimBloqueioAtual;
        }

        // Caso haja espaço disponível após o último bloqueio até o fim da jornada
        LocalDateTime fimDaJornada = request.getDataDesejada().atTime(jornada.getHoraFim());
        if (ponteiroAtual.isBefore(fimDaJornada)) {
            long duracaoFinalGapMinutos = java.time.Duration.between(ponteiroAtual, fimDaJornada).toMinutes();
            while (duracaoFinalGapMinutos >= duracaoTotalNecessaria) {
                LocalDateTime slotFim = ponteiroAtual.plusMinutes(duracaoServicos);
                if (slotFim.isAfter(fimDaJornada)) {
                    break; // Não cabe o serviço antes do fim da jornada
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


        // Garante que os horários estejam dentro da jornada (caso os bloqueios não cubram a totalidade)
        LocalTime inicioJornada = jornada.getHoraInicio();
        LocalTime fimJornada = jornada.getHoraFim();

        return horariosDisponiveis.stream()
                .filter(horario -> !horario.getHoraInicio().isBefore(inicioJornada) && !horario.getHoraFim().isAfter(fimJornada))
                .sorted(Comparator.comparing(HorarioDisponivelResponse::getHoraInicio)) // Garante ordem
                .collect(Collectors.toList());
    }

}
