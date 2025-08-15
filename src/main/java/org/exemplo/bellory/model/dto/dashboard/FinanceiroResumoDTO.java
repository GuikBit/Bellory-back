package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceiroResumoDTO {
    private BigDecimal receitaTotal;
    private BigDecimal receitaHoje;
    private BigDecimal receitaEsteMes;
    private BigDecimal receitaEsteAno;
    private BigDecimal receitaPrevista;
    private BigDecimal ticketMedio;
    private Map<String, BigDecimal> receitaPorServico;
    private Map<String, BigDecimal> receitaPorFuncionario;
    private BigDecimal receitaProdutos;
    private BigDecimal receitaServicos;
    private Long totalCobrancas;
    private Long cobrancasPagas;
    private Long cobrancasPendentes;
    private Long cobrancasVencidas;
    private BigDecimal valorPendente;
    private BigDecimal valorVencido;
    private Double percentualRecebimento;
}
