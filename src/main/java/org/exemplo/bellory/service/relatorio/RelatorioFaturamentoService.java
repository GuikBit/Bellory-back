package org.exemplo.bellory.service.relatorio;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.relatorio.RelatorioFaturamentoDTO;
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
public class RelatorioFaturamentoService {

    private final CobrancaRepository cobrancaRepository;

    public RelatorioFaturamentoDTO gerarRelatorio(RelatorioFiltroDTO filtro) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        validarFiltro(filtro);

        LocalDate dataInicio = filtro.getDataInicio() != null ? filtro.getDataInicio() : LocalDate.now().withDayOfMonth(1);
        LocalDate dataFim = filtro.getDataFim() != null ? filtro.getDataFim() : LocalDate.now();
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Faturamento bruto (total pago)
        BigDecimal faturamentoBruto = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                inicioDateTime, fimDateTime, organizacaoId);
        if (faturamentoBruto == null) faturamentoBruto = BigDecimal.ZERO;

        // Descontos aplicados
        BigDecimal descontos = cobrancaRepository.sumDescontosAplicadosByOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        if (descontos == null) descontos = BigDecimal.ZERO;

        // Total transacoes pagas
        Long totalTransacoes = 0L;
        List<Object[]> contagens = cobrancaRepository.countByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : contagens) {
            Cobranca.StatusCobranca status = (Cobranca.StatusCobranca) row[0];
            Long count = (Long) row[1];
            if (status == Cobranca.StatusCobranca.PAGO) {
                totalTransacoes = count;
            }
        }

        // Ticket medio
        BigDecimal ticketMedio = totalTransacoes > 0
                ? faturamentoBruto.divide(BigDecimal.valueOf(totalTransacoes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Faturamento por servico
        Map<String, BigDecimal> faturamentoPorServico = new LinkedHashMap<>();
        List<Object[]> receitaServicos = cobrancaRepository.sumReceitaByServicoAndOrganizacao(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : receitaServicos) {
            faturamentoPorServico.put((String) row[0], row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }

        // Faturamento por funcionario
        Map<String, BigDecimal> faturamentoPorFuncionario = new LinkedHashMap<>();
        List<Object[]> receitaFuncionarios = cobrancaRepository.sumReceitaByFuncionarioAndOrganizacao(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : receitaFuncionarios) {
            faturamentoPorFuncionario.put((String) row[0], row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }

        // Faturamento por forma de pagamento
        Map<String, BigDecimal> faturamentoPorFormaPagamento = new LinkedHashMap<>();
        List<Object[]> receitaFormas = cobrancaRepository.sumValorByFormaPagamentoAndOrganizacao(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : receitaFormas) {
            String forma = row[0] != null ? row[0].toString() : "OUTROS";
            faturamentoPorFormaPagamento.put(forma, row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }

        // Faturamento servicos vs produtos
        BigDecimal faturamentoServicos = BigDecimal.ZERO;
        BigDecimal faturamentoProdutos = BigDecimal.ZERO;
        List<Object[]> receitaPorTipo = cobrancaRepository.sumReceitaByTipoAndOrganizacao(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : receitaPorTipo) {
            Cobranca.TipoCobranca tipo = (Cobranca.TipoCobranca) row[0];
            BigDecimal valor = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            if (tipo == Cobranca.TipoCobranca.AGENDAMENTO) faturamentoServicos = valor;
            else if (tipo == Cobranca.TipoCobranca.COMPRA) faturamentoProdutos = valor;
        }

        // Periodo anterior para comparativo
        long diasPeriodo = ChronoUnit.DAYS.between(dataInicio, dataFim);
        LocalDate inicioAnterior = dataInicio.minusDays(diasPeriodo + 1);
        LocalDate fimAnterior = dataInicio.minusDays(1);
        BigDecimal faturamentoAnterior = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59), organizacaoId);
        if (faturamentoAnterior == null) faturamentoAnterior = BigDecimal.ZERO;

        Double percentualVariacao = calcularPercentualVariacao(faturamentoBruto, faturamentoAnterior);
        String tendencia = percentualVariacao > 5 ? "ALTA" : percentualVariacao < -5 ? "BAIXA" : "ESTAVEL";

        // Evolucao no periodo
        List<RelatorioFaturamentoDTO.FaturamentoPeriodoDTO> evolucao = calcularEvolucao(
                organizacaoId, dataInicio, dataFim);

        return RelatorioFaturamentoDTO.builder()
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .periodoConsulta(formatarPeriodo(dataInicio, dataFim))
                .faturamentoBruto(faturamentoBruto)
                .descontosAplicados(descontos)
                .faturamentoLiquido(faturamentoBruto.subtract(descontos))
                .ticketMedio(ticketMedio)
                .totalTransacoes(totalTransacoes)
                .faturamentoPeriodoAnterior(faturamentoAnterior)
                .percentualVariacao(percentualVariacao)
                .tendencia(tendencia)
                .faturamentoPorServico(faturamentoPorServico)
                .faturamentoPorFuncionario(faturamentoPorFuncionario)
                .faturamentoPorFormaPagamento(faturamentoPorFormaPagamento)
                .faturamentoServicos(faturamentoServicos)
                .faturamentoProdutos(faturamentoProdutos)
                .evolucao(evolucao)
                .build();
    }

    private List<RelatorioFaturamentoDTO.FaturamentoPeriodoDTO> calcularEvolucao(
            Long organizacaoId, LocalDate dataInicio, LocalDate dataFim) {
        List<RelatorioFaturamentoDTO.FaturamentoPeriodoDTO> evolucao = new ArrayList<>();

        List<Object[]> dados = cobrancaRepository.sumReceitaByDataAndOrganizacao(
                organizacaoId, dataInicio.atStartOfDay(), dataFim.atTime(23, 59, 59));

        for (Object[] row : dados) {
            java.sql.Date data = (java.sql.Date) row[0];
            BigDecimal valor = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            Long quantidade = row[2] != null ? ((Number) row[2]).longValue() : 0L;

            evolucao.add(RelatorioFaturamentoDTO.FaturamentoPeriodoDTO.builder()
                    .periodo(data.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .valor(valor)
                    .quantidade(quantidade)
                    .build());
        }

        return evolucao;
    }

    private Double calcularPercentualVariacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return atual != null && atual.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return atual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 2, RoundingMode.HALF_UP)
                .doubleValue();
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
