package org.exemplo.bellory.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCodeRequestDTO {
    @NotBlank(message = "Identificador (username ou email) é obrigatório")
    private String identifier;

    @NotBlank(message = "Código é obrigatório")
    @Size(min = 6, max = 6, message = "Código deve ter exatamente 6 dígitos")
    private String code;
}
