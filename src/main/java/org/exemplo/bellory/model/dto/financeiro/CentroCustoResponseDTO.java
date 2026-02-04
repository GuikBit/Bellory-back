package org.exemplo.bellory.model.dto.financeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.financeiro.CentroCusto;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CentroCustoResponseDTO {
    private Long id;
    private String nome;
    private String codigo;
    private String descricao;
    private Boolean ativo;

    public CentroCustoResponseDTO(CentroCusto entity) {
        this.id = entity.getId();
        this.nome = entity.getNome();
        this.codigo = entity.getCodigo();
        this.descricao = entity.getDescricao();
        this.ativo = entity.getAtivo();
    }
}
