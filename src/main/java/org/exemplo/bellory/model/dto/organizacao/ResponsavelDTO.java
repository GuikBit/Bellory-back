package org.exemplo.bellory.model.dto.organizacao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponsavelDTO {
    @NotBlank(message = "Nome do responsável é obrigatório")
    private String nome;

    @Email(message = "Email do responsável inválido")
    @NotBlank(message = "Email do responsável é obrigatório")
    private String email;

    @NotBlank(message = "Telefone do responsável é obrigatório")
    private String telefone;
}
