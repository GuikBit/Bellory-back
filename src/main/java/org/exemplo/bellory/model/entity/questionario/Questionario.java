package org.exemplo.bellory.model.entity.questionario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.questionario.enums.TipoQuestionario;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questionario", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Questionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoQuestionario tipo;

    @OneToMany(mappedBy = "questionario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    @Builder.Default
    private List<Pergunta> perguntas = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Builder.Default
    private Boolean obrigatorio = false;

    @Builder.Default
    private Boolean anonimo = false;

    @Column(name = "url_imagem", length = 500)
    private String urlImagem;

    @Column(name = "cor_tema", length = 7)
    private String corTema;

    @Column(name = "dt_criacao")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(name = "usuario_atualizacao")
    private String usuarioAtualizacao;

    @Column(name = "is_deletado", nullable = false)
    @Builder.Default
    private boolean isDeletado = false;

    @Column(name = "usuario_deletado")
    private String usuarioDeletado;

    @Column(name = "dt_deletado")
    private LocalDateTime dtDeletado;

    @Transient
    private Long totalRespostas;

    @PrePersist
    public void prePersist() {
        if (dtCriacao == null) {
            dtCriacao = LocalDateTime.now();
        }
        if (ativo == null) {
            ativo = true;
        }
        if (obrigatorio == null) {
            obrigatorio = false;
        }
        if (anonimo == null) {
            anonimo = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }

    public void addPergunta(Pergunta pergunta) {
        if (perguntas == null) {
            perguntas = new ArrayList<>();
        }
        perguntas.add(pergunta);
        pergunta.setQuestionario(this);
    }

    public void removePergunta(Pergunta pergunta) {
        perguntas.remove(pergunta);
        pergunta.setQuestionario(null);
    }

    public void clearPerguntas() {
        if (perguntas != null) {
            perguntas.clear();
        }
    }
}
