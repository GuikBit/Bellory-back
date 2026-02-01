package org.exemplo.bellory.model.dto.questionario;

import lombok.*;
import org.exemplo.bellory.model.entity.questionario.RespostaQuestionario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaQuestionarioDTO {

    private Long id;
    private Long questionarioId;
    private String questionarioTitulo;
    private Long clienteId;
    private String clienteNome;
    private Long colaboradorId;
    private String colaboradorNome;
    private Long agendamentoId;
    private List<RespostaPerguntaDTO> respostas;
    private LocalDateTime dtResposta;
    private String ipOrigem;
    private String dispositivo;
    private Integer tempoPreenchimentoSegundos;

    public RespostaQuestionarioDTO(RespostaQuestionario entity) {
        this.id = entity.getId();
        this.questionarioId = entity.getQuestionario() != null ? entity.getQuestionario().getId() : null;
        this.questionarioTitulo = entity.getQuestionario() != null ? entity.getQuestionario().getTitulo() : null;
        this.clienteId = entity.getClienteId();
        this.colaboradorId = entity.getColaboradorId();
        this.agendamentoId = entity.getAgendamentoId();
        this.dtResposta = entity.getDtResposta();
        this.ipOrigem = entity.getIpOrigem();
        this.dispositivo = entity.getDispositivo();
        this.tempoPreenchimentoSegundos = entity.getTempoPreenchimentoSegundos();

        if (entity.getRespostas() != null && !entity.getRespostas().isEmpty()) {
            this.respostas = entity.getRespostas().stream()
                    .map(RespostaPerguntaDTO::new)
                    .collect(Collectors.toList());
        }
    }
}
