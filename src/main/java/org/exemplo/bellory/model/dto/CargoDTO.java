package org.exemplo.bellory.model.dto;

import lombok.*;
import org.exemplo.bellory.model.entity.funcionario.Cargo;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CargoDTO {
    private Long id;
    private String nome;
    private String descricao;
    private boolean ativo;

    public CargoDTO(Cargo o) {
        id = o.getId();
        this.nome = o.getNome();
        this.descricao = o.getDescricao();
        this.ativo = o.isAtivo();
    }
}
