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
public class FluxoCaixaDTO {

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private BigDecimal saldoInicial;
    private BigDecimal totalReceitas;
    private BigDecimal totalDespesas;
    private BigDecimal saldoFinal;
    private BigDecimal saldoPrevisto;

    private List<FluxoCaixaDiaDTO> fluxoDiario = new ArrayList<>();
    private List<FluxoCaixaCategoriaDTO> receitasPorCategoria = new ArrayList<>();
    private List<FluxoCaixaCategoriaDTO> despesasPorCategoria = new ArrayList<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FluxoCaixaDiaDTO {
        private LocalDate data;
        private BigDecimal receitas;
        private BigDecimal despesas;
        private BigDecimal saldo;
        private BigDecimal saldoAcumulado;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FluxoCaixaCategoriaDTO {
        private Long categoriaId;
        private String categoriaNome;
        private BigDecimal valor;
        private BigDecimal percentual;
    }
}
