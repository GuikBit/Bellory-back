package org.exemplo.bellory.model.entity.assinatura;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento_plataforma", schema = "admin",
    indexes = {
        @Index(name = "idx_pagamento_plat_cobranca", columnList = "cobranca_id"),
        @Index(name = "idx_pagamento_plat_assas", columnList = "assas_payment_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoPlataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cobranca_id", nullable = false)
    private CobrancaPlataforma cobranca;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusPagamentoPlataforma status = StatusPagamentoPlataforma.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamentoPlataforma formaPagamento;

    // Dados da transacao
    @Column(name = "assas_payment_id", length = 100)
    private String assasPaymentId;

    @Column(name = "assas_transaction_id", length = 100)
    private String assasTransactionId;

    @Column(name = "comprovante_url", length = 500)
    private String comprovanteUrl;

    @Column(name = "dt_pagamento")
    private LocalDateTime dtPagamento;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
    }
}
