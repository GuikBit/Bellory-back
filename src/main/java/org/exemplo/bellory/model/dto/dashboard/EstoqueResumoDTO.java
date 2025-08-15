package org.exemplo.bellory.model.dto.dashboard;


import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstoqueResumoDTO {
    private Long totalProdutos;
    private Long produtosAtivos;
    private Long produtosEstoqueBaixo;
    private Long produtosSemEstoque;
    private BigDecimal valorTotalEstoque;
    private List<ProdutoEstoqueDTO> produtosBaixoEstoque;
    private List<ProdutoTopDTO> produtosMaisVendidos;
    private Double giroEstoque;
    private BigDecimal valorEstoqueParado;
    private Long alertasEstoque;
}
