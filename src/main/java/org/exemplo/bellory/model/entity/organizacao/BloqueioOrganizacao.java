package org.exemplo.bellory.model.entity.organizacao;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bloqueio_organizacao", schema = "app",
    indexes = {
        @Index(name = "idx_bloqueio_org_organizacao_id", columnList = "organizacao_id"),
        @Index(name = "idx_bloqueio_org_datas", columnList = "data_inicio, data_fim"),
        @Index(name = "idx_bloqueio_org_ativo", columnList = "organizacao_id, ativo"),
        @Index(name = "idx_bloqueio_org_ano", columnList = "organizacao_id, ano_referencia")
    })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BloqueioOrganizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoBloqueioOrganizacao tipo;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 50)
    private OrigemBloqueio origem;

    @Column(name = "ano_referencia")
    private Integer anoReferencia;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
        dtAtualizacao = LocalDateTime.now();
        if (ativo == null) {
            ativo = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }

    /**
     * Verifica se uma data específica está dentro do período de bloqueio
     */
    public boolean bloqueiaData(LocalDate data) {
        if (!ativo) return false;
        return !data.isBefore(dataInicio) && !data.isAfter(dataFim);
    }
}
