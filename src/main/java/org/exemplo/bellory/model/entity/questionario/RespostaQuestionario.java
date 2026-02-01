package org.exemplo.bellory.model.entity.questionario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resposta_questionario", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaQuestionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionario_id", nullable = false)
    @JsonIgnore
    private Questionario questionario;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "colaborador_id")
    private Long colaboradorId;

    @Column(name = "agendamento_id")
    private Long agendamentoId;

    @OneToMany(mappedBy = "respostaQuestionario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RespostaPergunta> respostas = new ArrayList<>();

    @Column(name = "dt_resposta", nullable = false)
    private LocalDateTime dtResposta;

    @Column(name = "ip_origem", length = 45)
    private String ipOrigem;

    @Column(length = 100)
    private String dispositivo;

    @Column(name = "tempo_preenchimento_segundos")
    private Integer tempoPreenchimentoSegundos;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    public void prePersist() {
        if (dtResposta == null) {
            dtResposta = LocalDateTime.now();
        }
    }

    public void addResposta(RespostaPergunta resposta) {
        if (respostas == null) {
            respostas = new ArrayList<>();
        }
        respostas.add(resposta);
        resposta.setRespostaQuestionario(this);
    }
}
