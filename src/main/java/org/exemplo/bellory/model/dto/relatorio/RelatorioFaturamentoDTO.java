package org.exemplo.bellory.model.dto.relatorio;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioFaturamentoDTO {

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String periodoConsulta;

    // Totais
    private BigDecimal faturamentoBruto;
    private BigDecimal descontosAplicados;
    private BigDecimal faturamentoLiquido;
    private BigDecimal ticketMedio;
    private Long totalTransacoes;

    // Comparativo com periodo anterior
    private BigDecimal faturamentoPeriodoAnterior;
    private Double percentualVariacao;
    private String tendencia; // ALTA, BAIXA, ESTAVEL

    // Detalhamentos
    private Map<String, BigDecimal> faturamentoPorServico;
    private Map<String, BigDecimal> faturamentoPorFuncionario;
    private Map<String, BigDecimal> faturamentoPorFormaPagamento;
    private BigDecimal faturamentoServicos;
    private BigDecimal faturamentoProdutos;

    // Evolucao no periodo
    private List<FaturamentoPeriodoDTO> evolucao;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FaturamentoPeriodoDTO {
        private String periodo;
        private BigDecimal valor;
        private Long quantidade;
    }
}
