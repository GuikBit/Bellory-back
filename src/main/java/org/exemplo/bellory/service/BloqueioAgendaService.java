package org.exemplo.bellory.service;

import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.BloqueioAgendaDTO;
import org.exemplo.bellory.model.dto.BloqueioOrganizacaoDTO;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.funcionario.BloqueioAgendaRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.BloqueioOrganizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BloqueioAgendaService {

    private final BloqueioAgendaRepository bloqueioAgendaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final BloqueioOrganizacaoRepository bloqueioOrganizacaoRepository;

    public BloqueioAgendaService(BloqueioAgendaRepository bloqueioAgendaRepository,
                                  FuncionarioRepository funcionarioRepository,
                                  AgendamentoRepository agendamentoRepository,
                                  BloqueioOrganizacaoRepository bloqueioOrganizacaoRepository) {
        this.bloqueioAgendaRepository = bloqueioAgendaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.bloqueioOrganizacaoRepository = bloqueioOrganizacaoRepository;
    }

    @Transactional
    public Object criarBloqueio(Long funcionarioId, BloqueioAgendaDTO dto) {
        Funcionario funcionario = buscarFuncionarioValidado(funcionarioId);

        validarCamposObrigatorios(dto);
        validarPeriodo(dto);

        TipoBloqueio tipo = parseTipoBloqueio(dto.getTipoBloqueio());

        // Validar horário de trabalho
        validarDentroJornada(funcionario, dto.getInicioBloqueio(), dto.getFimBloqueio());

        // Verificar bloqueios sobrepostos
        if (bloqueioAgendaRepository.existsBloqueioSobreposto(funcionarioId, dto.getInicioBloqueio(), dto.getFimBloqueio())) {
            throw new IllegalArgumentException("Já existe um bloqueio neste horário para o funcionário.");
        }

        // Verificar agendamentos conflitantes
        List<Agendamento> conflitos = buscarAgendamentosConflitantes(funcionarioId, dto.getInicioBloqueio(), dto.getFimBloqueio());
        if (!conflitos.isEmpty()) {
            List<Map<String, Object>> agendamentosResumo = conflitos.stream()
                    .map(this::mapAgendamentoResumo)
                    .collect(Collectors.toList());

            return Map.of(
                    "conflito", true,
                    "message", String.format("Existem %d agendamento(s) no período do bloqueio que precisam ser cancelados ou reagendados.", conflitos.size()),
                    "agendamentos", agendamentosResumo
            );
        }

        // Criar bloqueio
        BloqueioAgenda bloqueio = new BloqueioAgenda();
        bloqueio.setFuncionario(funcionario);
        bloqueio.setTitulo(dto.getTitulo());
        bloqueio.setInicioBloqueio(dto.getInicioBloqueio());
        bloqueio.setFimBloqueio(dto.getFimBloqueio());
        bloqueio.setDescricao(dto.getDescricao());
        bloqueio.setTipoBloqueio(tipo);

        BloqueioAgenda salvo = bloqueioAgendaRepository.save(bloqueio);
        return new BloqueioAgendaDTO(salvo);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listarBloqueiosPorPeriodo(Long funcionarioId, LocalDateTime inicio, LocalDateTime fim) {
        Funcionario funcionario = buscarFuncionarioValidado(funcionarioId);
        Long organizacaoId = funcionario.getOrganizacao().getId();

        List<BloqueioAgendaDTO> bloqueiosFuncionario = bloqueioAgendaRepository
                .findBloqueiosManuaisByFuncionarioIdAndPeriodo(funcionarioId, inicio, fim)
                .stream()
                .map(BloqueioAgendaDTO::new)
                .collect(Collectors.toList());

        LocalDate dataInicio = inicio.toLocalDate();
        LocalDate dataFim = fim.toLocalDate();

        List<BloqueioOrganizacaoDTO> bloqueiosOrganizacao = bloqueioOrganizacaoRepository
                .findBloqueiosAtivosNoPeriodo(organizacaoId, dataInicio, dataFim)
                .stream()
                .map(BloqueioOrganizacaoDTO::new)
                .collect(Collectors.toList());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("bloqueiosFuncionario", bloqueiosFuncionario);
        resultado.put("bloqueiosOrganizacao", bloqueiosOrganizacao);

        return resultado;
    }

    @Transactional
    public void removerBloqueio(Long funcionarioId, Long bloqueioId) {
        Funcionario funcionario = buscarFuncionarioValidado(funcionarioId);

        BloqueioAgenda bloqueio = bloqueioAgendaRepository.findById(bloqueioId)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueio com ID " + bloqueioId + " não encontrado."));

        if (!bloqueio.getFuncionario().getId().equals(funcionarioId)) {
            throw new SecurityException("Este bloqueio não pertence ao funcionário informado.");
        }

        if (bloqueio.getTipoBloqueio() == TipoBloqueio.AGENDAMENTO) {
            throw new IllegalArgumentException("Não é possível remover bloqueios do tipo AGENDAMENTO por esta rota.");
        }

        bloqueioAgendaRepository.delete(bloqueio);
    }

    // === VALIDAÇÕES ===

    private void validarCamposObrigatorios(BloqueioAgendaDTO dto) {
        if (dto.getInicioBloqueio() == null) {
            throw new IllegalArgumentException("O início do bloqueio é obrigatório.");
        }
        if (dto.getFimBloqueio() == null) {
            throw new IllegalArgumentException("O fim do bloqueio é obrigatório.");
        }
        if (dto.getTipoBloqueio() == null || dto.getTipoBloqueio().trim().isEmpty()) {
            throw new IllegalArgumentException("O tipo do bloqueio é obrigatório.");
        }
    }

    private void validarPeriodo(BloqueioAgendaDTO dto) {
        if (!dto.getFimBloqueio().isAfter(dto.getInicioBloqueio())) {
            throw new IllegalArgumentException("O fim do bloqueio deve ser posterior ao início.");
        }

        if (!dto.getInicioBloqueio().toLocalDate().equals(dto.getFimBloqueio().toLocalDate())) {
            throw new IllegalArgumentException("O bloqueio deve iniciar e terminar no mesmo dia.");
        }

        if (dto.getInicioBloqueio().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível criar bloqueios no passado.");
        }
    }

    private TipoBloqueio parseTipoBloqueio(String tipo) {
        try {
            TipoBloqueio parsed = TipoBloqueio.valueOf(tipo.toUpperCase());
            if (parsed == TipoBloqueio.AGENDAMENTO) {
                throw new IllegalArgumentException("Não é permitido criar bloqueios do tipo AGENDAMENTO manualmente.");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("AGENDAMENTO")) throw e;
            throw new IllegalArgumentException("Tipo de bloqueio inválido: '" + tipo + "'. Valores aceitos: ALMOCO, REUNIAO, PAUSA, FERIAS, FOLGA, OUTRO.");
        }
    }

    private void validarDentroJornada(Funcionario funcionario, LocalDateTime inicio, LocalDateTime fim) {
        DiaSemana diaSemana = DiaSemana.fromDayOfWeek(inicio.getDayOfWeek());

        JornadaDia jornadaDia = funcionario.getJornadasDia().stream()
                .filter(j -> j.getDiaSemana() == diaSemana && j.getAtivo())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "O funcionário não trabalha neste dia da semana (" + diaSemana.getDescricao() + "). " +
                        "Ajuste o plano de horários antes de criar o bloqueio."));

        List<HorarioTrabalho> horarios = jornadaDia.getHorarios();
        if (horarios == null || horarios.isEmpty()) {
            throw new IllegalArgumentException(
                    "O funcionário não possui horários cadastrados para " + diaSemana.getDescricao() + ".");
        }

        LocalTime inicioTime = inicio.toLocalTime();
        LocalTime fimTime = fim.toLocalTime();

        LocalTime jornadaInicio = horarios.stream()
                .map(HorarioTrabalho::getHoraInicio)
                .min(LocalTime::compareTo)
                .get();

        LocalTime jornadaFim = horarios.stream()
                .map(HorarioTrabalho::getHoraFim)
                .max(LocalTime::compareTo)
                .get();

        if (inicioTime.isBefore(jornadaInicio) || fimTime.isAfter(jornadaFim)) {
            throw new IllegalArgumentException(String.format(
                    "O bloqueio (%s - %s) está fora do horário de trabalho do funcionário (%s - %s) para %s.",
                    inicioTime, fimTime, jornadaInicio, jornadaFim, diaSemana.getDescricao()));
        }
    }

    private List<Agendamento> buscarAgendamentosConflitantes(Long funcionarioId, LocalDateTime inicio, LocalDateTime fim) {
        LocalDateTime inicioDia = inicio.toLocalDate().atStartOfDay();
        LocalDateTime fimDia = inicio.toLocalDate().atTime(LocalTime.MAX);

        List<Agendamento> agendamentosDoDia = agendamentoRepository
                .findAtivosByFuncionarioAndDataRange(funcionarioId, inicioDia, fimDia);

        LocalTime bloqueioInicio = inicio.toLocalTime();
        LocalTime bloqueioFim = fim.toLocalTime();

        List<Agendamento> conflitos = new ArrayList<>();
        for (Agendamento ag : agendamentosDoDia) {
            LocalTime agInicio = ag.getDtAgendamento().toLocalTime();

            int duracaoMinutos = ag.getServicos() != null
                    ? ag.getServicos().stream()
                        .mapToInt(s -> s.getTempoEstimadoMinutos() != null ? s.getTempoEstimadoMinutos() : 0)
                        .sum()
                    : 30;

            LocalTime agFim = agInicio.plusMinutes(duracaoMinutos);

            // Overlap: ag_inicio < bloqueio_fim AND ag_fim > bloqueio_inicio
            if (agInicio.isBefore(bloqueioFim) && agFim.isAfter(bloqueioInicio)) {
                conflitos.add(ag);
            }
        }

        return conflitos;
    }

    private Map<String, Object> mapAgendamentoResumo(Agendamento ag) {
        int duracaoMinutos = ag.getServicos() != null
                ? ag.getServicos().stream()
                    .mapToInt(s -> s.getTempoEstimadoMinutos() != null ? s.getTempoEstimadoMinutos() : 0)
                    .sum()
                : 30;

        LocalDateTime fimEstimado = ag.getDtAgendamento().plusMinutes(duracaoMinutos);

        List<String> servicos = ag.getServicos() != null
                ? ag.getServicos().stream().map(s -> s.getNome()).collect(Collectors.toList())
                : List.of();

        return Map.of(
                "id", ag.getId(),
                "cliente", ag.getCliente().getNomeCompleto(),
                "servicos", servicos,
                "inicio", ag.getDtAgendamento().toString(),
                "fimEstimado", fimEstimado.toString(),
                "status", ag.getStatus().name()
        );
    }

    // === AUXILIARES ===

    private Funcionario buscarFuncionarioValidado(Long funcionarioId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + funcionarioId + " não encontrado."));

        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada no token.");
        }
        if (!organizacaoId.equals(funcionario.getOrganizacao().getId())) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso.");
        }

        return funcionario;
    }
}
