package org.exemplo.bellory.model.dto.organizacao;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcessoAdmDTO {
    @NotBlank(message = "Login do administrador é obrigatório")
    private String login;

    @NotBlank(message = "Senha do administrador é obrigatória")
    private String senha;

    // ✅ Role com valor padrão se não vier no JSON
    private String role = "ROLE_ADMIN";
}
