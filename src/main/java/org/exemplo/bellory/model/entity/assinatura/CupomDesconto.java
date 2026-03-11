package org.exemplo.bellory.model.entity.assinatura;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupom_desconto", schema = "admin",
    indexes = {
        @Index(name = "idx_cupom_desconto_codigo", columnList = "codigo"),
        @Index(name = "idx_cupom_desconto_ativo", columnList = "ativo")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CupomDesconto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_desconto", nullable = false, length = 20)
    private TipoDesconto tipoDesconto;

    @Column(name = "valor_desconto", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDesconto;

    @Column(name = "dt_inicio")
    private LocalDateTime dtInicio;

    @Column(name = "dt_fim")
    private LocalDateTime dtFim;

    @Column(name = "max_utilizacoes")
    private Integer maxUtilizacoes;

    @Column(name = "max_utilizacoes_por_org")
    private Integer maxUtilizacoesPorOrg;

    @Column(name = "total_utilizado", nullable = false)
    @Builder.Default
    private Integer totalUtilizado = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "planos_permitidos", columnDefinition = "jsonb")
    private String planosPermitidos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "segmentos_permitidos", columnDefinition = "jsonb")
    private String segmentosPermitidos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "organizacoes_permitidas", columnDefinition = "jsonb")
    private String organizacoesPermitidas;

    @Column(name = "ciclo_cobranca", length = 10)
    private String cicloCobranca;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_aplicacao", nullable = false, length = 20)
    @Builder.Default
    private TipoAplicacaoCupom tipoAplicacao = TipoAplicacaoCupom.PRIMEIRA_COBRANCA;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(name = "user_criacao")
    private Long userCriacao;

    @Column(name = "user_atualizacao")
    private Long userAtualizacao;

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }

    public boolean isVigente() {
        if (!Boolean.TRUE.equals(ativo)) return false;
        LocalDateTime agora = LocalDateTime.now();
        if (dtInicio != null && agora.isBefore(dtInicio)) return false;
        if (dtFim != null && agora.isAfter(dtFim)) return false;
        return true;
    }

    public boolean atingiuLimiteGlobal() {
        return maxUtilizacoes != null && totalUtilizado >= maxUtilizacoes;
    }
}
