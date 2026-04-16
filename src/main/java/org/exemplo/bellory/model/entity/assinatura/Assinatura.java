package org.exemplo.bellory.model.entity.assinatura;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;

/**
 * Vinculo 1:1 entre uma {@link Organizacao} do Bellory e sua assinatura na Payment API externa.
 * Todo o estado de cobranca/status/plano/limites e consultado via Payment API (cache Redis em
 * {@code AssinaturaCacheService}). Aqui guardamos apenas o par (customerId, subscriptionId).
 */
@Entity
@Table(name = "assinatura", schema = "admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false, unique = true)
    private Organizacao organizacao;

    @Column(name = "payment_api_customer_id")
    private Long paymentApiCustomerId;

    @Column(name = "payment_api_subscription_id")
    private Long paymentApiSubscriptionId;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
    }
}
