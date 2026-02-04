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
public class DREDTO {

    private LocalDate dataInicio;
    private LocalDate dataFim;

    // Receitas
    private BigDecimal receitaBruta;
    private BigDecimal descontosReceita;
    private BigDecimal receitaLiquida;

    // Despesas
    private BigDecimal totalDespesasOperacionais;
    private BigDecimal totalDespesasAdministrativas;
    private BigDecimal totalDespesasComPessoal;
    private BigDecimal totalOutrasDespesas;
    private BigDecimal totalDespesas;

    // Resultado
    private BigDecimal resultadoOperacional;
    private BigDecimal resultadoLiquido;
    private BigDecimal margemOperacional;
    private BigDecimal margemLiquida;

    // Detalhamento
    private List<DRELinhaDTO> receitas = new ArrayList<>();
    private List<DRELinhaDTO> despesas = new ArrayList<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DRELinhaDTO {
        private Long categoriaId;
        private String categoriaNome;
        private String tipo;
        private BigDecimal valor;
        private BigDecimal percentual;
    }
}
