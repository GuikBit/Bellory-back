package org.exemplo.bellory.model.dto;

import lombok.*;
import org.exemplo.bellory.model.entity.agendamento.AgendamentoQuestionario;
import org.exemplo.bellory.model.entity.agendamento.StatusQuestionarioAgendamento;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionarioAgendamentoDTO {

    // ID do vínculo (AgendamentoQuestionario), usado para compor a URL pública de resposta.
    private Long id;
    private Long questionarioId;
    private String titulo;
    private StatusQuestionarioAgendamento status;
    private LocalDateTime dtEnvio;
    private LocalDateTime dtResposta;
    private Long respostaQuestionarioId;

    public QuestionarioAgendamentoDTO(AgendamentoQuestionario aq) {
        this.id = aq.getId();
        this.questionarioId = aq.getQuestionario() != null ? aq.getQuestionario().getId() : null;
        this.titulo = aq.getQuestionario() != null ? aq.getQuestionario().getTitulo() : null;
        this.status = aq.getStatus();
        this.dtEnvio = aq.getDtEnvio();
        this.dtResposta = aq.getDtResposta();
        this.respostaQuestionarioId = aq.getRespostaQuestionarioId();
    }
}
