package org.exemplo.bellory.model.dto.financeiro;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BalancoFinanceiroDTO {

    private LocalDate dataReferencia;

    // Resumo
    private BigDecimal totalAtivos;
    private BigDecimal totalContasReceber;
    private BigDecimal totalContasPagar;
    private BigDecimal saldoLiquido;

    // Contas Bancárias
    private List<SaldoContaBancariaDTO> saldosContas = new ArrayList<>();

    // Contas a Receber (resumo)
    private BigDecimal contasReceberPendentes;
    private BigDecimal contasReceberVencidas;
    private BigDecimal contasReceberRecebidas;
    private int qtdContasReceberPendentes;
    private int qtdContasReceberVencidas;

    // Contas a Pagar (resumo)
    private BigDecimal contasPagarPendentes;
    private BigDecimal contasPagarVencidas;
    private BigDecimal contasPagarPagas;
    private int qtdContasPagarPendentes;
    private int qtdContasPagarVencidas;

    // Indicadores
    private BigDecimal indiceLiquidez;
    private BigDecimal ticketMedioReceitas;
    private BigDecimal ticketMedioDespesas;
    private BigDecimal receitaMediaDiaria;

    // Evolução mensal
    private List<BalancoMensalDTO> evolucaoMensal = new ArrayList<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SaldoContaBancariaDTO {
        private Long contaId;
        private String contaNome;
        private String tipoConta;
        private String banco;
        private BigDecimal saldoAtual;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BalancoMensalDTO {
        private String mesAno;
        private BigDecimal receitas;
        private BigDecimal despesas;
        private BigDecimal resultado;
    }
}
