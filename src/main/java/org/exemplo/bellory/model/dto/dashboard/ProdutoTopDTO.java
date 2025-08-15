package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoTopDTO {
    private Long id;
    private String nome;
    private Long quantidadeVendida;
    private BigDecimal valorTotal;
    private String categoria;
}
