package org.exemplo.bellory.model.dto.questionario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaQuestionarioCreateDTO {

    @NotNull(message = "ID do questionário é obrigatório")
    private Long questionarioId;

    private Long clienteId;

    private Long colaboradorId;

    private Long agendamentoId;

    @Valid
    @NotEmpty(message = "É necessário enviar pelo menos uma resposta")
    private List<RespostaPerguntaCreateDTO> respostas;

    // Metadados opcionais
    private String userAgent;
    private String dispositivo;
    private Integer tempoPreenchimentoSegundos;
}
