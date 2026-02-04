package org.exemplo.bellory.model.dto.financeiro;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardFinanceiroDTO {

    // Resumo do dia
    private BigDecimal receitasHoje;
    private BigDecimal despesasHoje;
    private BigDecimal saldoHoje;

    // Resumo do mês
    private BigDecimal receitasMes;
    private BigDecimal despesasMes;
    private BigDecimal saldoMes;

    // Saldo total contas
    private BigDecimal saldoTotalContas;

    // Contas vencidas
    private int contasPagarVencidas;
    private BigDecimal valorContasPagarVencidas;
    private int contasReceberVencidas;
    private BigDecimal valorContasReceberVencidas;

    // Contas a vencer (próximos 7 dias)
    private int contasPagarAVencer;
    private BigDecimal valorContasPagarAVencer;
    private int contasReceberAVencer;
    private BigDecimal valorContasReceberAVencer;

    // Evolução receitas vs despesas (últimos 12 meses)
    private List<EvolucaoMensalDTO> evolucao = new ArrayList<>();

    // Top categorias despesas do mês
    private List<TopCategoriaDTO> topCategoriasDespesas = new ArrayList<>();

    // Top categorias receitas do mês
    private List<TopCategoriaDTO> topCategoriasReceitas = new ArrayList<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class EvolucaoMensalDTO {
        private String mesAno;
        private BigDecimal receitas;
        private BigDecimal despesas;
        private BigDecimal resultado;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TopCategoriaDTO {
        private Long categoriaId;
        private String categoriaNome;
        private BigDecimal valor;
        private BigDecimal percentual;
    }
}
