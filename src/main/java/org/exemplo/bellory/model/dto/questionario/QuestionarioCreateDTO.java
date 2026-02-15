package org.exemplo.bellory.model.dto.questionario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.exemplo.bellory.model.entity.questionario.enums.TipoQuestionario;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionarioCreateDTO {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 255, message = "Título deve ter no máximo 255 caracteres")
    private String titulo;

    @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    private String descricao;

    @NotNull(message = "Tipo é obrigatório")
    private TipoQuestionario tipo;

    @Valid
    private List<PerguntaCreateDTO> perguntas;

    private Boolean ativo;

    private Boolean obrigatorio;

    private Boolean anonimo;

    private String urlImagem;

    private String corTema;
}
