package org.exemplo.bellory.model.dto.questionario;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaPerguntaCreateDTO {

    @NotNull(message = "ID da pergunta é obrigatório")
    private Long perguntaId;

    private String respostaTexto;

    private BigDecimal respostaNumero;

    private List<Long> respostaOpcaoIds;

    private LocalDate respostaData;

    private LocalTime respostaHora;
}
