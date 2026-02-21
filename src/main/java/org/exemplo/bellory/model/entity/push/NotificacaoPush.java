package org.exemplo.bellory.model.entity.push;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao_push", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_role", nullable = false, length = 30)
    private String userRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "origem", length = 100)
    private String origem;

    @Column(name = "detalhe", columnDefinition = "TEXT")
    private String detalhe;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", length = 10)
    private PrioridadeNotificacao prioridade = PrioridadeNotificacao.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 30)
    private CategoriaNotificacao categoria = CategoriaNotificacao.SISTEMA;

    @Column(name = "lido")
    private Boolean lido = false;

    @Column(name = "icone")
    private String icone;

    @Column(name = "url_acao", length = 500)
    private String urlAcao;

    @Column(name = "is_sw")
    private Boolean isSw = false;

    @Column(name = "dt_cadastro")
    private LocalDateTime dtCadastro;

    @Column(name = "dt_read")
    private LocalDateTime dtRead;

    @PrePersist
    protected void onCreate() {
        this.dtCadastro = LocalDateTime.now();
    }
}
