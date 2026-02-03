package org.exemplo.bellory.service.relatorio;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.relatorio.RelatorioFiltroDTO;
import org.exemplo.bellory.model.dto.relatorio.RelatorioServicoDTO;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RelatorioServicoService {

    private final AgendamentoRepository agendamentoRepository;

    public RelatorioServicoDTO gerarRelatorio(RelatorioFiltroDTO filtro) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        validarFiltro(filtro);

        LocalDate dataInicio = filtro.getDataInicio() != null ? filtro.getDataInicio() : LocalDate.now().withDayOfMonth(1);
        LocalDate dataFim = filtro.getDataFim() != null ? filtro.getDataFim() : LocalDate.now();
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Servicos detalhados
        List<Object[]> dados = agendamentoRepository.countServicosDetalhadosByOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);

        Long totalServicosRealizados = 0L;
        BigDecimal faturamentoTotal = BigDecimal.ZERO;
        List<RelatorioServicoDTO.ServicoDetalhadoDTO> ranking = new ArrayList<>();

        // Agregar por categoria
        Map<String, Long> qtdPorCategoria = new LinkedHashMap<>();
        Map<String, BigDecimal> fatPorCategoria = new LinkedHashMap<>();
        Map<String, List<String>> servicosPorCategoria = new LinkedHashMap<>();

        for (Object[] row : dados) {
            Long id = (Long) row[0];
            String nome = (String) row[1];
            String categoria = row[2] != null ? (String) row[2] : "Sem categoria";
            Long quantidade = (Long) row[3];
            BigDecimal faturamento = row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO;
            Integer tempoEstimado = row[5] != null ? (Integer) row[5] : null;

            totalServicosRealizados += quantidade;
            faturamentoTotal = faturamentoTotal.add(faturamento);

            ranking.add(RelatorioServicoDTO.ServicoDetalhadoDTO.builder()
                    .id(id)
                    .nome(nome)
                    .categoria(categoria)
                    .quantidadeRealizada(quantidade)
                    .faturamento(faturamento)
                    .precoMedio(quantidade > 0
                            ? faturamento.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO)
                    .tempoEstimadoMinutos(tempoEstimado)
                    .build());

            qtdPorCategoria.merge(categoria, quantidade, Long::sum);
            fatPorCategoria.merge(categoria, faturamento, BigDecimal::add);
            servicosPorCategoria.computeIfAbsent(categoria, k -> new ArrayList<>()).add(nome);
        }

        // Calcular percentuais
        final long totalFinal = totalServicosRealizados;
        final BigDecimal fatFinal = faturamentoTotal;
        ranking.forEach(s -> s.setPercentualDoTotal(
                totalFinal > 0 ? (s.getQuantidadeRealizada() * 100.0) / totalFinal : 0.0));

        // Ticket medio
        BigDecimal ticketMedioServico = totalServicosRealizados > 0
                ? faturamentoTotal.divide(BigDecimal.valueOf(totalServicosRealizados), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Desempenho por categoria
        List<RelatorioServicoDTO.CategoriaDesempenhoDTO> categorias = new ArrayList<>();
        for (String cat : qtdPorCategoria.keySet()) {
            BigDecimal fatCat = fatPorCategoria.getOrDefault(cat, BigDecimal.ZERO);
            Double pctFat = fatFinal.compareTo(BigDecimal.ZERO) > 0
                    ? fatCat.multiply(BigDecimal.valueOf(100)).divide(fatFinal, 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            List<String> topServs = servicosPorCategoria.getOrDefault(cat, List.of());
            if (topServs.size() > 5) topServs = topServs.subList(0, 5);

            categorias.add(RelatorioServicoDTO.CategoriaDesempenhoDTO.builder()
                    .categoria(cat)
                    .totalServicos(qtdPorCategoria.get(cat))
                    .faturamento(fatCat)
                    .percentualFaturamento(pctFat)
                    .topServicos(topServs)
                    .build());
        }
        categorias.sort((a, b) -> b.getFaturamento().compareTo(a.getFaturamento()));

        // Evolucao - agrupada por data (simplificado usando servicos mais vendidos por periodo)
        List<RelatorioServicoDTO.ServicoPeriodoDTO> evolucao = new ArrayList<>();
        List<Object[]> evolucaoData = agendamentoRepository.countByDataAndStatusAndOrganizacao(
                organizacaoId, inicioDateTime, fimDateTime);

        Map<String, Long> qtdPorData = new LinkedHashMap<>();
        for (Object[] row : evolucaoData) {
            java.sql.Date data = (java.sql.Date) row[0];
            Long count = ((Number) row[2]).longValue();
            String dataFormatada = data.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            qtdPorData.merge(dataFormatada, count, Long::sum);
        }

        for (Map.Entry<String, Long> entry : qtdPorData.entrySet()) {
            evolucao.add(RelatorioServicoDTO.ServicoPeriodoDTO.builder()
                    .periodo(entry.getKey())
                    .quantidade(entry.getValue())
                    .build());
        }

        return RelatorioServicoDTO.builder()
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .periodoConsulta(formatarPeriodo(dataInicio, dataFim))
                .rankingServicos(ranking)
                .desempenhoPorCategoria(categorias)
                .totalServicosRealizados(totalServicosRealizados)
                .faturamentoTotalServicos(faturamentoTotal)
                .ticketMedioServico(ticketMedioServico)
                .evolucao(evolucao)
                .build();
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
