package org.exemplo.bellory.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequestDTO {
    @NotBlank(message = "Identificador (username ou email) é obrigatório")
    private String identifier;
}
