package org.exemplo.bellory.model.dto;

import lombok.*;
import org.exemplo.bellory.model.dto.questionario.RespostaQuestionarioDTO;
import org.exemplo.bellory.model.entity.agendamento.AgendamentoQuestionario;
import org.exemplo.bellory.model.entity.agendamento.StatusQuestionarioAgendamento;

import java.time.LocalDateTime;

/**
 * Visão completa de um questionário (anamnese) vinculado a um agendamento, incluindo
 * a resposta do cliente quando já preenchida. Pensado para a tela de detalhes do
 * agendamento no admin: 1 chamada → renderiza tudo (status + perguntas + respostas).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendamentoQuestionarioDetalheDTO {

    /** ID do vínculo AgendamentoQuestionario. */
    private Long id;
    private Long agendamentoId;
    private Long questionarioId;
    private String questionarioTitulo;
    private String questionarioDescricao;
    private StatusQuestionarioAgendamento status;
    private LocalDateTime dtEnvio;
    private LocalDateTime dtResposta;

    /** Resposta completa (perguntas + respostas) — null enquanto cliente não respondeu. */
    private RespostaQuestionarioDTO resposta;

    public static AgendamentoQuestionarioDetalheDTO of(AgendamentoQuestionario aq, RespostaQuestionarioDTO resposta) {
        return AgendamentoQuestionarioDetalheDTO.builder()
                .id(aq.getId())
                .agendamentoId(aq.getAgendamento() != null ? aq.getAgendamento().getId() : null)
                .questionarioId(aq.getQuestionario() != null ? aq.getQuestionario().getId() : null)
                .questionarioTitulo(aq.getQuestionario() != null ? aq.getQuestionario().getTitulo() : null)
                .questionarioDescricao(aq.getQuestionario() != null ? aq.getQuestionario().getDescricao() : null)
                .status(aq.getStatus())
                .dtEnvio(aq.getDtEnvio())
                .dtResposta(aq.getDtResposta())
                .resposta(resposta)
                .build();
    }
}
