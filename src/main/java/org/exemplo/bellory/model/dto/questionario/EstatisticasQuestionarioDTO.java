package org.exemplo.bellory.model.dto.questionario;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstatisticasQuestionarioDTO {

    private Long questionarioId;
    private String questionarioTitulo;

    // Métricas gerais
    private Long totalRespostas;
    private Long respostasHoje;
    private Long respostasUltimos7Dias;
    private Long respostasUltimos30Dias;
    private Double mediaTempoPreenchimentoSegundos;
    private Double taxaConclusao;

    // Período
    private LocalDateTime primeiraResposta;
    private LocalDateTime ultimaResposta;

    // Estatísticas por pergunta
    private List<EstatisticasPerguntaDTO> estatisticasPerguntas;

    // Distribuição temporal
    private Map<String, Long> respostasPorDia;
    private Map<String, Long> respostasPorHora;
}
