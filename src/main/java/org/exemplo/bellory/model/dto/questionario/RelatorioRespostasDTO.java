package org.exemplo.bellory.model.dto.questionario;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioRespostasDTO {

    private Long questionarioId;
    private String questionarioTitulo;
    private LocalDateTime dataGeracao;
    private LocalDateTime periodoInicio;
    private LocalDateTime periodoFim;

    // Resumo
    private Long totalRespostas;
    private Long totalClientes;
    private Long totalAgendamentosAvaliados;

    // NPS (Net Promoter Score) - se aplicável
    private Double npsScore;
    private Long promotores;
    private Long neutros;
    private Long detratores;

    // Média geral de satisfação
    private Double mediaSatisfacao;

    // Detalhamento
    private List<RespostaQuestionarioDTO> respostas;
    private EstatisticasQuestionarioDTO estatisticas;
}
