package org.exemplo.bellory.model.dto.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AtualizarClientePublicoDTO {

    @Email(message = "Email inválido.")
    @NotBlank(message = "Email é obrigatório.")
    private String email;
}
