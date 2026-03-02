package org.exemplo.bellory.model.entity.assinatura;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "assinatura", schema = "admin",
    indexes = {
        @Index(name = "idx_assinatura_status", columnList = "status"),
        @Index(name = "idx_assinatura_dt_proximo_vencimento", columnList = "dt_proximo_vencimento")
    })
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_bellory_id", nullable = false)
    private PlanoBellory planoBellory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusAssinatura status = StatusAssinatura.TRIAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "ciclo_cobranca", nullable = false, length = 10)
    @Builder.Default
    private CicloCobranca cicloCobranca = CicloCobranca.MENSAL;

    // Trial
    @Column(name = "dt_inicio_trial")
    private LocalDateTime dtInicioTrial;

    @Column(name = "dt_fim_trial")
    private LocalDateTime dtFimTrial;

    // Assinatura ativa
    @Column(name = "dt_inicio")
    private LocalDateTime dtInicio;

    @Column(name = "dt_proximo_vencimento")
    private LocalDateTime dtProximoVencimento;

    @Column(name = "dt_cancelamento")
    private LocalDateTime dtCancelamento;

    // Valores
    @Column(name = "valor_mensal", precision = 10, scale = 2)
    private BigDecimal valorMensal;

    @Column(name = "valor_anual", precision = 10, scale = 2)
    private BigDecimal valorAnual;

    // Integracao Assas
    @Column(name = "assas_customer_id", length = 100)
    private String assasCustomerId;

    @Column(name = "assas_subscription_id", length = 100)
    private String assasSubscriptionId;

    // Auditoria
    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }

    public boolean isTrialExpirado() {
        return status == StatusAssinatura.TRIAL
                && dtFimTrial != null
                && LocalDateTime.now().isAfter(dtFimTrial);
    }

    public boolean isBloqueada() {
        return status == StatusAssinatura.VENCIDA
                || status == StatusAssinatura.CANCELADA
                || status == StatusAssinatura.SUSPENSA;
    }
}
