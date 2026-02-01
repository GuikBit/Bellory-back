package org.exemplo.bellory.model.dto.questionario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerguntaCreateDTO {

    @NotBlank(message = "Texto da pergunta é obrigatório")
    @Size(max = 500, message = "Texto deve ter no máximo 500 caracteres")
    private String texto;

    @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    private String descricao;

    @NotNull(message = "Tipo da pergunta é obrigatório")
    private TipoPergunta tipo;

    private Boolean obrigatoria;

    @NotNull(message = "Ordem é obrigatória")
    private Integer ordem;

    @Valid
    private List<OpcaoRespostaCreateDTO> opcoes;

    // Campos para tipo ESCALA
    private Integer escalaMin;
    private Integer escalaMax;
    private String labelMin;
    private String labelMax;

    // Campos para validação
    private Integer minCaracteres;
    private Integer maxCaracteres;
    private Double minValor;
    private Double maxValor;
}
