package org.exemplo.bellory.model.dto.financeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaFinanceiraCreateDTO {
    private Long categoriaPaiId;
    private String nome;
    private String descricao;
    private String tipo; // RECEITA, DESPESA
    private String cor;
    private String icone;
}
