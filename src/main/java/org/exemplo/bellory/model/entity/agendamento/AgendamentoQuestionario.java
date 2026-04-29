package org.exemplo.bellory.model.entity.agendamento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.questionario.Questionario;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "agendamento_questionario",
        schema = "app",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_aq_ag_q",
                columnNames = {"agendamento_id", "questionario_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendamentoQuestionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", nullable = false)
    @JsonIgnore
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "questionario_id", nullable = false)
    private Questionario questionario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusQuestionarioAgendamento status = StatusQuestionarioAgendamento.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_assinatura", nullable = false, length = 20)
    @Builder.Default
    private StatusAssinatura statusAssinatura = StatusAssinatura.NAO_REQUERIDA;

    @Column(name = "dt_envio")
    private LocalDateTime dtEnvio;

    @Column(name = "dt_resposta")
    private LocalDateTime dtResposta;

    @Column(name = "dt_assinatura")
    private LocalDateTime dtAssinatura;

    @Column(name = "resposta_questionario_id")
    private Long respostaQuestionarioId;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime dtCriacao;

    @PrePersist
    public void prePersist() {
        if (dtCriacao == null) {
            dtCriacao = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusQuestionarioAgendamento.PENDENTE;
        }
        if (statusAssinatura == null) {
            statusAssinatura = StatusAssinatura.NAO_REQUERIDA;
        }
    }
}
