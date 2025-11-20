package org.exemplo.bellory.model.dto.tema;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemaDTO {
    // ✅ Ignora o campo "id" do JSON (Embeddable não tem ID)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String id;

    @NotBlank(message = "Nome do tema é obrigatório")
    private String nome;

    @NotBlank(message = "Tipo do tema é obrigatório")
    private String tipo;

    @Valid
    @NotNull(message = "Cores são obrigatórias")
    private CoresDTO cores;

    // ✅ Campos opcionais com valores padrão
    private FontsDTO fonts;
    private BorderRadiusDTO borderRadius;
    private ShadowsDTO shadows;
}
