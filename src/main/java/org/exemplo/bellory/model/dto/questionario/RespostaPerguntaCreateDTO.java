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

    // ===== Termo de consentimento =====

    /** Marcado true quando o cliente aceita o termo (checkbox "Li e concordo"). */
    private Boolean aceitouTermo;

    /**
     * Texto do termo com placeholders ja substituidos pelo front (snapshot do que o cliente viu).
     * O servidor calcula o hash e congela.
     */
    private String textoTermoRenderizado;

    // ===== Assinatura digital =====

    /** Data URL no formato {@code data:image/png;base64,...} ou {@code data:image/svg+xml;base64,...}. */
    private String assinaturaClienteBase64;

    /** Idem, quando a pergunta exige assinatura do profissional responsavel. */
    private String assinaturaProfissionalBase64;
}
