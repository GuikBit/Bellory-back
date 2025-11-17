package org.exemplo.bellory.model.dto;

import lombok.*;
import org.exemplo.bellory.model.entity.funcionario.Cargo;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CargoDTO {
    private String nome;
    private String descricao;

    public CargoDTO(Cargo o) {
        this.nome = o.getNome();
        this.descricao = o.getDescricao();
    }
}
