package org.exemplo.bellory.model.dto.questionario;

import lombok.*;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstatisticasPerguntaDTO {

    private Long perguntaId;
    private String perguntaTexto;
    private TipoPergunta tipo;
    private Long totalRespostas;
    private Long respostasEmBranco;

    // Para perguntas numéricas/escala/estrelas
    private Double media;
    private Double mediana;
    private Double moda;
    private Double desvioPadrao;
    private Double valorMinimo;
    private Double valorMaximo;

    // Distribuição de notas (para escala/estrelas)
    private Map<Integer, Long> distribuicaoNotas;

    // Para perguntas de seleção
    private List<EstatisticaOpcaoDTO> estatisticasOpcoes;

    // Para perguntas sim/não
    private Long totalSim;
    private Long totalNao;
    private Double percentualSim;

    // Para texto (análise básica)
    private Double mediaCaracteres;
    private List<String> palavrasFrequentes;
}
