package org.exemplo.bellory.model.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para representar produtos mais vendidos no dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoVendidoDTO {
    private Long id;
    private String nome;
    private Long quantidadeVendida;
    private BigDecimal receitaGerada;
    private BigDecimal precoUnitario;
}
