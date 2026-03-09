package org.exemplo.bellory.model.entity.assinatura;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_log", schema = "admin",
    indexes = {
        @Index(name = "idx_webhook_log_assinatura", columnList = "assinatura_id"),
        @Index(name = "idx_webhook_log_evento", columnList = "evento"),
        @Index(name = "idx_webhook_log_assas_payment", columnList = "assas_payment_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id")
    private Assinatura assinatura;

    @Column(nullable = false, length = 50)
    private String evento;

    @Column(name = "assas_payment_id", length = 100)
    private String assasPaymentId;

    @Column(name = "assas_subscription_id", length = 100)
    private String assasSubscriptionId;

    @Column(precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "status_pagamento", length = 30)
    private String statusPagamento;

    @Column(name = "payload_resumo", columnDefinition = "TEXT")
    private String payloadResumo;

    @Column(name = "dt_recebimento", nullable = false, updatable = false)
    private LocalDateTime dtRecebimento;

    @PrePersist
    protected void onCreate() {
        if (dtRecebimento == null) {
            dtRecebimento = LocalDateTime.now();
        }
    }
}
