package org.exemplo.bellory.model.dto.questionario;

import lombok.*;
import org.exemplo.bellory.model.entity.questionario.OpcaoResposta;
import org.exemplo.bellory.model.entity.questionario.RespostaPergunta;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaPerguntaDTO {

    private Long id;
    private Long perguntaId;
    private String perguntaTexto;
    private TipoPergunta tipoPergunta;
    private String respostaTexto;
    private BigDecimal respostaNumero;
    private List<OpcaoSelecionadaDTO> opcoesSelecionadas;
    private LocalDate respostaData;
    private LocalTime respostaHora;
    private String respostaFormatada;

    public RespostaPerguntaDTO(RespostaPergunta entity) {
        this.id = entity.getId();
        this.perguntaId = entity.getPergunta() != null ? entity.getPergunta().getId() : null;
        this.perguntaTexto = entity.getPergunta() != null ? entity.getPergunta().getTexto() : null;
        this.tipoPergunta = entity.getPergunta() != null ? entity.getPergunta().getTipo() : null;
        this.respostaTexto = entity.getRespostaTexto();
        this.respostaNumero = entity.getRespostaNumero();
        this.respostaData = entity.getRespostaData();
        this.respostaHora = entity.getRespostaHora();

        // Mapear opções selecionadas
        if (entity.getRespostaOpcaoIds() != null && !entity.getRespostaOpcaoIds().isEmpty()
                && entity.getPergunta() != null && entity.getPergunta().getOpcoes() != null) {
            this.opcoesSelecionadas = entity.getPergunta().getOpcoes().stream()
                    .filter(o -> entity.getRespostaOpcaoIds().contains(o.getId()))
                    .map(o -> new OpcaoSelecionadaDTO(o.getId(), o.getTexto(), o.getValor()))
                    .collect(Collectors.toList());
        }

        // Formatar resposta
        this.respostaFormatada = formatarResposta(entity);
    }

    private String formatarResposta(RespostaPergunta resposta) {
        if (resposta.getPergunta() == null) return null;

        TipoPergunta tipo = resposta.getPergunta().getTipo();

        switch (tipo) {
            case TEXTO_CURTO:
            case TEXTO_LONGO:
            case SIM_NAO:
                return resposta.getRespostaTexto();

            case NUMERO:
            case ESCALA:
                return resposta.getRespostaNumero() != null ?
                        String.valueOf(resposta.getRespostaNumero().intValue()) : null;

            case AVALIACAO_ESTRELAS:
                if (resposta.getRespostaNumero() != null) {
                    int stars = resposta.getRespostaNumero().intValue();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < stars; i++) sb.append("★");
                    for (int i = stars; i < 5; i++) sb.append("☆");
                    return sb.toString();
                }
                return null;

            case SELECAO_UNICA:
            case SELECAO_MULTIPLA:
                if (resposta.getRespostaOpcaoIds() != null && resposta.getPergunta().getOpcoes() != null) {
                    return resposta.getPergunta().getOpcoes().stream()
                            .filter(o -> resposta.getRespostaOpcaoIds().contains(o.getId()))
                            .map(OpcaoResposta::getTexto)
                            .collect(Collectors.joining(", "));
                }
                return null;

            case DATA:
                return resposta.getRespostaData() != null ?
                        resposta.getRespostaData().toString() : null;

            case HORA:
                return resposta.getRespostaHora() != null ?
                        resposta.getRespostaHora().toString() : null;

            default:
                return null;
        }
    }
}
