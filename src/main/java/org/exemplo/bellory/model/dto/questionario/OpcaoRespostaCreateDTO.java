package org.exemplo.bellory.model.dto.questionario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcaoRespostaCreateDTO {

    @NotBlank(message = "Texto da opção é obrigatório")
    @Size(max = 255, message = "Texto deve ter no máximo 255 caracteres")
    private String texto;

    private String valor;

    @NotNull(message = "Ordem é obrigatória")
    private Integer ordem;
}
