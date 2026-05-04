package org.exemplo.bellory.model.entity.importacao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "cliente_importacao", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteImportacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    // Quem disparou a importacao (pode ser null se vier de fonte sem auth).
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "nome_arquivo")
    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusImportacao status = StatusImportacao.PENDENTE;

    @Column(name = "total_linhas", nullable = false)
    private Integer totalLinhas = 0;

    @Column(name = "processadas", nullable = false)
    private Integer processadas = 0;

    @Column(name = "importados", nullable = false)
    private Integer importados = 0;

    @Column(name = "ignorados", nullable = false)
    private Integer ignorados = 0;

    // JSONB com [{linha: int, motivo: string}, ...]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "erros", columnDefinition = "jsonb")
    private String erros;

    @Column(name = "mensagem_falha", columnDefinition = "TEXT")
    private String mensagemFalha;

    @Column(name = "dt_inicio", nullable = false)
    private LocalDateTime dtInicio;

    @Column(name = "dt_fim")
    private LocalDateTime dtFim;

    @PrePersist
    protected void onCreate() {
        if (dtInicio == null) {
            dtInicio = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusImportacao.PENDENTE;
        }
    }
}
