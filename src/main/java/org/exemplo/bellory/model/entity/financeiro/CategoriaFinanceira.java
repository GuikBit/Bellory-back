package org.exemplo.bellory.model.entity.financeiro;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categoria_financeira", schema = "app", indexes = {
    @Index(name = "idx_catfin_org_ativo", columnList = "organizacao_id, ativo")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaFinanceira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_pai_id")
    @JsonIgnore
    private CategoriaFinanceira categoriaPai;

    @OneToMany(mappedBy = "categoriaPai", fetch = FetchType.LAZY)
    private List<CategoriaFinanceira> subcategorias = new ArrayList<>();

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCategoria tipo;

    @Column(length = 7)
    private String cor;

    @Column(length = 50)
    private String icone;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    // === ENUMS ===
    public enum TipoCategoria {
        RECEITA("Receita"),
        DESPESA("Despesa");

        private final String descricao;

        TipoCategoria(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // === LIFECYCLE ===
    @PrePersist
    public void prePersist() {
        if (this.dtCriacao == null) {
            this.dtCriacao = LocalDateTime.now();
        }
        if (this.ativo == null) {
            this.ativo = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }

    // === MÉTODOS DE CONVENIÊNCIA ===
    public boolean isReceita() {
        return this.tipo == TipoCategoria.RECEITA;
    }

    public boolean isDespesa() {
        return this.tipo == TipoCategoria.DESPESA;
    }

    public boolean hasSubcategorias() {
        return this.subcategorias != null && !this.subcategorias.isEmpty();
    }

    public boolean isCategoriaPrincipal() {
        return this.categoriaPai == null;
    }
}
