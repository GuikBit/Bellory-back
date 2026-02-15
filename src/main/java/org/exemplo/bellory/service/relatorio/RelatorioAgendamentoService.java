package org.exemplo.bellory.service.relatorio;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.relatorio.RelatorioAgendamentoDTO;
import org.exemplo.bellory.model.dto.relatorio.RelatorioFiltroDTO;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RelatorioAgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final CobrancaRepository cobrancaRepository;

    private static final String[] DIAS_SEMANA = {"Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};

    public RelatorioAgendamentoDTO gerarRelatorio(RelatorioFiltroDTO filtro) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        validarFiltro(filtro);

        LocalDate dataInicio = filtro.getDataInicio() != null ? filtro.getDataInicio() : LocalDate.now().withDayOfMonth(1);
        LocalDate dataFim = filtro.getDataFim() != null ? filtro.getDataFim() : LocalDate.now();
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Contagem total
        Long totalAgendamentos = agendamentoRepository.countByOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        if (totalAgendamentos == null) totalAgendamentos = 0L;

        // Por status
        Map<String, Long> porStatus = new LinkedHashMap<>();
        List<Object[]> statusCounts = agendamentoRepository.countByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : statusCounts) {
            Status status = (Status) row[0];
            Long count = (Long) row[1];
            porStatus.put(status.name(), count);
        }

        Long concluidos = porStatus.getOrDefault("CONCLUIDO", 0L);
        Long cancelados = porStatus.getOrDefault("CANCELADO", 0L);
        Long naoCompareceu = porStatus.getOrDefault("NAO_COMPARECEU", 0L);
        Long reagendados = porStatus.getOrDefault("REAGENDADO", 0L);
        Long pendentes = porStatus.getOrDefault("PENDENTE", 0L) +
                porStatus.getOrDefault("AGENDADO", 0L) +
                porStatus.getOrDefault("EM_ESPERA", 0L);

        // Taxas
        Double taxaConclusao = totalAgendamentos > 0 ? (concluidos * 100.0) / totalAgendamentos : 0.0;
        Double taxaCancelamento = totalAgendamentos > 0 ? (cancelados * 100.0) / totalAgendamentos : 0.0;
        Double taxaNoShow = totalAgendamentos > 0 ? (naoCompareceu * 100.0) / totalAgendamentos : 0.0;
        Double taxaReagendamento = totalAgendamentos > 0 ? (reagendados * 100.0) / totalAgendamentos : 0.0;

        // Por dia da semana
        Map<String, Long> porDiaSemana = new LinkedHashMap<>();
        List<Object[]> diaSemanaData = agendamentoRepository.countByDiaSemanaAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : diaSemanaData) {
            int dow = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            String diaNome = dow >= 0 && dow < DIAS_SEMANA.length ? DIAS_SEMANA[dow] : "Dia " + dow;
            porDiaSemana.put(diaNome, count);
        }

        // Por horario
        Map<String, Long> porHorario = new LinkedHashMap<>();
        List<Object[]> horaData = agendamentoRepository.countByHoraAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : horaData) {
            int hora = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            porHorario.put(String.format("%02d:00", hora), count);
        }

        // Top servicos
        List<RelatorioAgendamentoDTO.ServicoRankingDTO> servicosMaisAgendados = new ArrayList<>();
        List<Object[]> servicosData = agendamentoRepository.countServicosMaisVendidosByOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : servicosData) {
            Long id = (Long) row[0];
            String nome = (String) row[1];
            Long qtd = (Long) row[2];
            Double pctTotal = totalAgendamentos > 0 ? (qtd * 100.0) / totalAgendamentos : 0.0;

            servicosMaisAgendados.add(RelatorioAgendamentoDTO.ServicoRankingDTO.builder()
                    .id(id)
                    .nome(nome)
                    .quantidade(qtd)
                    .percentualTotal(pctTotal)
                    .build());
            if (servicosMaisAgendados.size() >= 10) break;
        }

        // Top funcionarios com detalhes de status
        List<RelatorioAgendamentoDTO.FuncionarioRankingDTO> funcionarios = montarRankingFuncionarios(
                organizacaoId, inicioDateTime, fimDateTime);

        // Evolucao no periodo
        List<RelatorioAgendamentoDTO.AgendamentoPeriodoDTO> evolucao = calcularEvolucao(
                organizacaoId, inicioDateTime, fimDateTime);

        // Comparativo com periodo anterior
        long diasPeriodo = ChronoUnit.DAYS.between(dataInicio, dataFim);
        LocalDate inicioAnterior = dataInicio.minusDays(diasPeriodo + 1);
        LocalDate fimAnterior = dataInicio.minusDays(1);
        Long totalAnterior = agendamentoRepository.countByOrganizacaoAndPeriodo(
                organizacaoId, inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59));
        if (totalAnterior == null) totalAnterior = 0L;
        Double percentualVariacao = totalAnterior > 0
                ? ((totalAgendamentos - totalAnterior) * 100.0) / totalAnterior : 0.0;

        return RelatorioAgendamentoDTO.builder()
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .periodoConsulta(formatarPeriodo(dataInicio, dataFim))
                .totalAgendamentos(totalAgendamentos)
                .agendamentosConcluidos(concluidos)
                .agendamentosCancelados(cancelados)
                .agendamentosNaoCompareceu(naoCompareceu)
                .agendamentosReagendados(reagendados)
                .agendamentosPendentes(pendentes)
                .taxaConclusao(taxaConclusao)
                .taxaCancelamento(taxaCancelamento)
                .taxaNoShow(taxaNoShow)
                .taxaReagendamento(taxaReagendamento)
                .porStatus(porStatus)
                .porDiaSemana(porDiaSemana)
                .porHorario(porHorario)
                .evolucao(evolucao)
                .servicosMaisAgendados(servicosMaisAgendados)
                .funcionariosMaisAtendimentos(funcionarios)
                .totalPeriodoAnterior(totalAnterior)
                .percentualVariacao(percentualVariacao)
                .build();
    }

    private List<RelatorioAgendamentoDTO.FuncionarioRankingDTO> montarRankingFuncionarios(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        List<Object[]> dados = agendamentoRepository.countByFuncionarioAndStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);

        // Agrupa por funcionario
        Map<Long, RelatorioAgendamentoDTO.FuncionarioRankingDTO.FuncionarioRankingDTOBuilder> builders = new LinkedHashMap<>();

        for (Object[] row : dados) {
            Long funcId = (Long) row[0];
            String nome = (String) row[1];
            Status status = (Status) row[2];
            Long count = (Long) row[3];

            RelatorioAgendamentoDTO.FuncionarioRankingDTO.FuncionarioRankingDTOBuilder builder =
                    builders.computeIfAbsent(funcId, k -> RelatorioAgendamentoDTO.FuncionarioRankingDTO.builder()
                            .id(funcId)
                            .nome(nome)
                            .totalAtendimentos(0L)
                            .concluidos(0L)
                            .cancelados(0L));

            // Atualiza os valores — construímos depois
            // Para simplificar, coletamos em um mapa auxiliar
        }

        // Abordagem simplificada usando map auxiliar
        Map<Long, String> nomes = new LinkedHashMap<>();
        Map<Long, Long> totais = new LinkedHashMap<>();
        Map<Long, Long> concluidosMap = new LinkedHashMap<>();
        Map<Long, Long> canceladosMap = new LinkedHashMap<>();

        for (Object[] row : dados) {
            Long funcId = (Long) row[0];
            String nome = (String) row[1];
            Status status = (Status) row[2];
            Long count = (Long) row[3];

            nomes.putIfAbsent(funcId, nome);
            totais.merge(funcId, count, Long::sum);

            if (status == Status.CONCLUIDO) {
                concluidosMap.merge(funcId, count, Long::sum);
            } else if (status == Status.CANCELADO) {
                canceladosMap.merge(funcId, count, Long::sum);
            }
        }

        List<RelatorioAgendamentoDTO.FuncionarioRankingDTO> result = new ArrayList<>();
        for (Long funcId : nomes.keySet()) {
            Long total = totais.getOrDefault(funcId, 0L);
            Long conc = concluidosMap.getOrDefault(funcId, 0L);
            Long canc = canceladosMap.getOrDefault(funcId, 0L);
            Double taxa = total > 0 ? (conc * 100.0) / total : 0.0;

            result.add(RelatorioAgendamentoDTO.FuncionarioRankingDTO.builder()
                    .id(funcId)
                    .nome(nomes.get(funcId))
                    .totalAtendimentos(total)
                    .concluidos(conc)
                    .cancelados(canc)
                    .taxaConclusao(taxa)
                    .build());
        }

        result.sort((a, b) -> Long.compare(b.getTotalAtendimentos(), a.getTotalAtendimentos()));
        return result.size() > 10 ? result.subList(0, 10) : result;
    }

    private List<RelatorioAgendamentoDTO.AgendamentoPeriodoDTO> calcularEvolucao(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        List<Object[]> dados = agendamentoRepository.countByDataAndStatusAndOrganizacao(
                organizacaoId, inicio, fim);

        Map<String, Long> totaisPorData = new LinkedHashMap<>();
        Map<String, Long> concluidosPorData = new LinkedHashMap<>();
        Map<String, Long> canceladosPorData = new LinkedHashMap<>();

        for (Object[] row : dados) {
            java.sql.Date data = (java.sql.Date) row[0];
            String statusStr = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            String dataFormatada = data.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            totaisPorData.merge(dataFormatada, count, Long::sum);
            if ("CONCLUIDO".equals(statusStr)) {
                concluidosPorData.merge(dataFormatada, count, Long::sum);
            } else if ("CANCELADO".equals(statusStr)) {
                canceladosPorData.merge(dataFormatada, count, Long::sum);
            }
        }

        List<RelatorioAgendamentoDTO.AgendamentoPeriodoDTO> evolucao = new ArrayList<>();
        for (String data : totaisPorData.keySet()) {
            evolucao.add(RelatorioAgendamentoDTO.AgendamentoPeriodoDTO.builder()
                    .periodo(data)
                    .total(totaisPorData.getOrDefault(data, 0L))
                    .concluidos(concluidosPorData.getOrDefault(data, 0L))
                    .cancelados(canceladosPorData.getOrDefault(data, 0L))
                    .build());
        }

        return evolucao;
    }

    private void validarFiltro(RelatorioFiltroDTO filtro) {
        if (filtro.getDataInicio() != null && filtro.getDataFim() != null) {
            if (filtro.getDataInicio().isAfter(filtro.getDataFim())) {
                throw new IllegalArgumentException("Data de início não pode ser posterior à data de fim.");
            }
        }
    }

    private String formatarPeriodo(LocalDate inicio, LocalDate fim) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return inicio.format(formatter) + " a " + fim.format(formatter);
    }

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }
        return organizacaoId;
    }
}
