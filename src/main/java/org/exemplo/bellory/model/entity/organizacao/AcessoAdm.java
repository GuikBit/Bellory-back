package org.exemplo.bellory.model.entity.organizacao;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Embeddable  // ADICIONADO: Marca esta classe como embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcessoAdm {
    @NotBlank(message = "Login do administrador é obrigatório")
    private String login;

    @NotBlank(message = "Senha do administrador é obrigatória")
    private String senha;

    @NotBlank(message = "Role é obrigatória")
    private String role;
}

