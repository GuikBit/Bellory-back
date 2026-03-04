package org.exemplo.bellory.model.entity.assinatura;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupom_utilizacao", schema = "admin",
    indexes = {
        @Index(name = "idx_cupom_utilizacao_cupom", columnList = "cupom_id"),
        @Index(name = "idx_cupom_utilizacao_org", columnList = "organizacao_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CupomUtilizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cupom_id", nullable = false)
    private CupomDesconto cupom;

    @Column(name = "organizacao_id", nullable = false)
    private Long organizacaoId;

    @Column(name = "assinatura_id")
    private Long assinaturaId;

    @Column(name = "cobranca_id")
    private Long cobrancaId;

    @Column(name = "valor_original", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorOriginal;

    @Column(name = "valor_desconto", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDesconto;

    @Column(name = "valor_final", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorFinal;

    @Column(name = "plano_codigo", length = 50)
    private String planoCodigo;

    @Column(name = "ciclo_cobranca", length = 10)
    private String cicloCobranca;

    @Column(name = "dt_utilizacao", nullable = false)
    @Builder.Default
    private LocalDateTime dtUtilizacao = LocalDateTime.now();
}
