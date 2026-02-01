package org.exemplo.bellory.model.entity.questionario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pergunta", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pergunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionario_id", nullable = false)
    @JsonIgnore
    private Questionario questionario;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoPergunta tipo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean obrigatoria = false;

    @Column(nullable = false)
    private Integer ordem;

    @OneToMany(mappedBy = "pergunta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    @Builder.Default
    private List<OpcaoResposta> opcoes = new ArrayList<>();

    // Campos para tipo ESCALA
    @Column(name = "escala_min")
    private Integer escalaMin;

    @Column(name = "escala_max")
    private Integer escalaMax;

    @Column(name = "label_min", length = 100)
    private String labelMin;

    @Column(name = "label_max", length = 100)
    private String labelMax;

    // Campos para validação de texto
    @Column(name = "min_caracteres")
    private Integer minCaracteres;

    @Column(name = "max_caracteres")
    private Integer maxCaracteres;

    // Campos para validação numérica
    @Column(name = "min_valor")
    private Double minValor;

    @Column(name = "max_valor")
    private Double maxValor;

    public void addOpcao(OpcaoResposta opcao) {
        if (opcoes == null) {
            opcoes = new ArrayList<>();
        }
        opcoes.add(opcao);
        opcao.setPergunta(this);
    }

    public void removeOpcao(OpcaoResposta opcao) {
        opcoes.remove(opcao);
        opcao.setPergunta(null);
    }

    public void clearOpcoes() {
        if (opcoes != null) {
            opcoes.clear();
        }
    }
}
