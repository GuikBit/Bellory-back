package org.exemplo.bellory.service.relatorio;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.relatorio.RelatorioCobrancaDTO;
import org.exemplo.bellory.model.dto.relatorio.RelatorioFiltroDTO;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
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
public class RelatorioCobrancaService {

    private final CobrancaRepository cobrancaRepository;

    public RelatorioCobrancaDTO gerarRelatorio(RelatorioFiltroDTO filtro) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        validarFiltro(filtro);

        LocalDate dataInicio = filtro.getDataInicio() != null ? filtro.getDataInicio() : LocalDate.now().withDayOfMonth(1);
        LocalDate dataFim = filtro.getDataFim() != null ? filtro.getDataFim() : LocalDate.now();
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Quantidade e valor por status
        Map<String, Long> quantidadePorStatus = new LinkedHashMap<>();
        Map<String, BigDecimal> valorPorStatus = new LinkedHashMap<>();

        List<Object[]> contagens = cobrancaRepository.countByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : contagens) {
            Cobranca.StatusCobranca status = (Cobranca.StatusCobranca) row[0];
            Long count = (Long) row[1];
            quantidadePorStatus.put(status.name(), count);
        }

        List<Object[]> valores = cobrancaRepository.sumValorByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : valores) {
            Cobranca.StatusCobranca status = (Cobranca.StatusCobranca) row[0];
            BigDecimal valor = (BigDecimal) row[1];
            valorPorStatus.put(status.name(), valor != null ? valor : BigDecimal.ZERO);
        }

        // Totais
        Long totalCobrancas = quantidadePorStatus.values().stream().reduce(0L, Long::sum);
        BigDecimal valorTotal = valorPorStatus.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorRecebido = valorPorStatus.getOrDefault("PAGO", BigDecimal.ZERO);
        BigDecimal valorPendente = cobrancaRepository.sumValorPendenteByOrganizacao(organizacaoId);
        if (valorPendente == null) valorPendente = BigDecimal.ZERO;
        BigDecimal valorVencido = cobrancaRepository.sumValorVencidoByOrganizacao(organizacaoId);
        if (valorVencido == null) valorVencido = BigDecimal.ZERO;
        BigDecimal valorCancelado = valorPorStatus.getOrDefault("CANCELADA", BigDecimal.ZERO)
                .add(valorPorStatus.getOrDefault("ESTORNADA", BigDecimal.ZERO));

        // Taxas
        Double taxaRecebimento = valorTotal.compareTo(BigDecimal.ZERO) > 0
                ? valorRecebido.multiply(BigDecimal.valueOf(100)).divide(valorTotal, 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        Double taxaInadimplencia = valorTotal.compareTo(BigDecimal.ZERO) > 0
                ? valorVencido.multiply(BigDecimal.valueOf(100)).divide(valorTotal, 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        // Por tipo
        Map<String, Long> quantidadePorTipo = new LinkedHashMap<>();
        Map<String, BigDecimal> valorPorTipo = new LinkedHashMap<>();
        List<Object[]> estatisticasTipo = cobrancaRepository.findEstatisticasPorTipo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : estatisticasTipo) {
            Cobranca.TipoCobranca tipo = (Cobranca.TipoCobranca) row[0];
            Long count = (Long) row[1];
            BigDecimal valor = (BigDecimal) row[2];
            quantidadePorTipo.put(tipo.name(), count);
            valorPorTipo.put(tipo.name(), valor != null ? valor : BigDecimal.ZERO);
        }

        // Sinais
        RelatorioCobrancaDTO.SinaisResumoDTO sinais = montarSinais(organizacaoId, inicioDateTime, fimDateTime);

        // Cobrancas vencidas detalhadas
        List<RelatorioCobrancaDTO.CobrancaVencidaDTO> cobrancasVencidas = montarCobrancasVencidas(organizacaoId);

        // Evolucao
        List<RelatorioCobrancaDTO.CobrancaPeriodoDTO> evolucao = montarEvolucao(
                organizacaoId, inicioDateTime, fimDateTime);

        return RelatorioCobrancaDTO.builder()
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .periodoConsulta(formatarPeriodo(dataInicio, dataFim))
                .totalCobrancas(totalCobrancas)
                .valorTotal(valorTotal)
                .valorRecebido(valorRecebido)
                .valorPendente(valorPendente)
                .valorVencido(valorVencido)
                .valorCancelado(valorCancelado)
                .taxaRecebimento(taxaRecebimento)
                .taxaInadimplencia(taxaInadimplencia)
                .quantidadePorStatus(quantidadePorStatus)
                .valorPorStatus(valorPorStatus)
                .quantidadePorTipo(quantidadePorTipo)
                .valorPorTipo(valorPorTipo)
                .sinais(sinais)
                .cobrancasVencidas(cobrancasVencidas)
                .evolucao(evolucao)
                .build();
    }

    private RelatorioCobrancaDTO.SinaisResumoDTO montarSinais(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        List<Object[]> sinaisData = cobrancaRepository.countSinaisByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);

        Long totalSinais = 0L;
        BigDecimal valorTotalSinais = BigDecimal.ZERO;
        Long sinaisPagos = 0L;
        BigDecimal valorSinaisPagos = BigDecimal.ZERO;
        Long sinaisPendentes = 0L;
        BigDecimal valorSinaisPendentes = BigDecimal.ZERO;

        for (Object[] row : sinaisData) {
            Cobranca.StatusCobranca status = (Cobranca.StatusCobranca) row[0];
            Long count = (Long) row[1];
            BigDecimal valor = (BigDecimal) row[2];
            totalSinais += count;
            valorTotalSinais = valorTotalSinais.add(valor != null ? valor : BigDecimal.ZERO);

            if (status == Cobranca.StatusCobranca.PAGO) {
                sinaisPagos = count;
                valorSinaisPagos = valor != null ? valor : BigDecimal.ZERO;
            } else if (status == Cobranca.StatusCobranca.PENDENTE || status == Cobranca.StatusCobranca.PARCIALMENTE_PAGO) {
                sinaisPendentes += count;
                valorSinaisPendentes = valorSinaisPendentes.add(valor != null ? valor : BigDecimal.ZERO);
            }
        }

        Double percentualMedioSinal = cobrancaRepository.avgPercentualSinalByOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        if (percentualMedioSinal == null) percentualMedioSinal = 0.0;

        return RelatorioCobrancaDTO.SinaisResumoDTO.builder()
                .totalSinais(totalSinais)
                .valorTotalSinais(valorTotalSinais)
                .sinaisPagos(sinaisPagos)
                .valorSinaisPagos(valorSinaisPagos)
                .sinaisPendentes(sinaisPendentes)
                .valorSinaisPendentes(valorSinaisPendentes)
                .percentualMedioSinal(percentualMedioSinal)
                .build();
    }

    private List<RelatorioCobrancaDTO.CobrancaVencidaDTO> montarCobrancasVencidas(Long organizacaoId) {
        List<RelatorioCobrancaDTO.CobrancaVencidaDTO> vencidas = new ArrayList<>();

        List<Object[]> dados = cobrancaRepository.findCobrancasVencidasDetalhadasByOrganizacao(organizacaoId);
        LocalDate hoje = LocalDate.now();

        for (Object[] row : dados) {
            LocalDate dtVencimento = (LocalDate) row[5];
            long diasAtraso = ChronoUnit.DAYS.between(dtVencimento, hoje);

            vencidas.add(RelatorioCobrancaDTO.CobrancaVencidaDTO.builder()
                    .id((Long) row[0])
                    .numeroCobranca((String) row[1])
                    .clienteNome((String) row[2])
                    .valor((BigDecimal) row[3])
                    .valorPendente((BigDecimal) row[4])
                    .dtVencimento(dtVencimento)
                    .diasAtraso(diasAtraso)
                    .tipo(row[6] != null ? row[6].toString() : null)
                    .build());

            if (vencidas.size() >= 50) break;
        }

        return vencidas;
    }

    private List<RelatorioCobrancaDTO.CobrancaPeriodoDTO> montarEvolucao(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        List<Object[]> dados = cobrancaRepository.sumReceitaByDataAndOrganizacao(
                organizacaoId, inicio, fim);

        List<RelatorioCobrancaDTO.CobrancaPeriodoDTO> evolucao = new ArrayList<>();
        for (Object[] row : dados) {
            java.sql.Date data = (java.sql.Date) row[0];
            BigDecimal valorRecebido = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            Long quantidade = row[2] != null ? ((Number) row[2]).longValue() : 0L;

            evolucao.add(RelatorioCobrancaDTO.CobrancaPeriodoDTO.builder()
                    .periodo(data.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .valorRecebido(valorRecebido)
                    .quantidade(quantidade)
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
