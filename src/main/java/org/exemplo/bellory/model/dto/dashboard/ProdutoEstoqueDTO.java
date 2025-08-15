package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoEstoqueDTO {
    private Long id;
    private String nome;
    private Integer quantidadeAtual;
    private Integer estoqueMinimo;
    private String categoria;
    private String status; // "CRITICO", "BAIXO", "OK"
}
