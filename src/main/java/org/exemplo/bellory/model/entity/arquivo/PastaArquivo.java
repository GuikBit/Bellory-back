package org.exemplo.bellory.model.entity.arquivo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pasta_arquivo", schema = "app",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organizacao_id", "caminho_completo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastaArquivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pasta_pai_id")
    @JsonIgnore
    private PastaArquivo pastaPai;

    @OneToMany(mappedBy = "pastaPai", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PastaArquivo> subpastas = new ArrayList<>();

    @OneToMany(mappedBy = "pasta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Arquivo> arquivos = new ArrayList<>();

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(name = "caminho_completo", nullable = false, length = 1000)
    private String caminhoCompleto;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dtAtualizacao;

    @Column(name = "criado_por")
    private Long criadoPor;

    @PrePersist
    protected void onCreate() {
        this.dtCriacao = LocalDateTime.now();
        this.dtAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }
}
