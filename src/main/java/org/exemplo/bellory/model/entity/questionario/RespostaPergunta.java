package org.exemplo.bellory.model.entity.questionario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resposta_pergunta", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaPergunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resposta_questionario_id", nullable = false)
    @JsonIgnore
    private RespostaQuestionario respostaQuestionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pergunta_id", nullable = false)
    private Pergunta pergunta;

    @Column(name = "resposta_texto", length = 5000)
    private String respostaTexto;

    @Column(name = "resposta_numero")
    private Double respostaNumero;

    @ElementCollection
    @CollectionTable(
        name = "resposta_opcoes_selecionadas",
        schema = "app",
        joinColumns = @JoinColumn(name = "resposta_pergunta_id")
    )
    @Column(name = "opcao_id")
    @Builder.Default
    private List<Long> respostaOpcaoIds = new ArrayList<>();

    @Column(name = "resposta_data")
    private LocalDate respostaData;

    @Column(name = "resposta_hora")
    private LocalTime respostaHora;
}
