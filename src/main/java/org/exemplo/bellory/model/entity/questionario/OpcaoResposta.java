package org.exemplo.bellory.model.entity.questionario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "opcao_resposta", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcaoResposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pergunta_id", nullable = false)
    @JsonIgnore
    private Pergunta pergunta;

    @Column(nullable = false, length = 255)
    private String texto;

    @Column(length = 255)
    private String valor;

    @Column(nullable = false)
    private Integer ordem;
}
