package org.exemplo.bellory.model.entity.arquivo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;

@Entity
@Table(name = "arquivo", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Arquivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pasta_id")
    private PastaArquivo pasta;

    @Column(name = "nome_original", nullable = false, length = 500)
    private String nomeOriginal;

    @Column(name = "nome_armazenado", nullable = false, length = 500)
    private String nomeArmazenado;

    @Column(name = "caminho_relativo", nullable = false, length = 1000)
    private String caminhoRelativo;

    @Column(length = 20)
    private String extensao;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(nullable = false)
    private Long tamanho;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dtCriacao;

    @Column(name = "criado_por")
    private Long criadoPor;

    @PrePersist
    protected void onCreate() {
        this.dtCriacao = LocalDateTime.now();
    }
}
