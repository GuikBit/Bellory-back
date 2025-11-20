package org.exemplo.bellory.model.entity.organizacao;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable  // ADICIONADO: Marca esta classe como embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Responsavel {
    @NotBlank(message = "Nome do responsável é obrigatório")
    private String nome;

    @Email(message = "Email do responsável inválido")
    @NotBlank(message = "Email do responsável é obrigatório")
    private String email;

    @NotBlank(message = "Telefone do responsável é obrigatório")
    private String telefone;
}
