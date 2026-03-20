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
    @Column(nullable = false, length = 30)
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

    @Column(name = "dt_trial_notificado")
    private LocalDateTime dtTrialNotificado;

    // Assinatura ativa
    @Column(name = "dt_inicio")
    private LocalDateTime dtInicio;

    @Column(name = "dt_proximo_vencimento")
    private LocalDateTime dtProximoVencimento;

    @Column(name = "dt_cancelamento")
    private LocalDateTime dtCancelamento;

    // Forma de pagamento preferida (para renovacao automatica)
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 20)
    private FormaPagamentoPlataforma formaPagamento;

    // Cupom de desconto
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cupom_id")
    private CupomDesconto cupom;

    @Column(name = "valor_desconto", precision = 10, scale = 2)
    private BigDecimal valorDesconto;

    @Column(name = "cupom_codigo", length = 50)
    private String cupomCodigo;

    // Integracao Assas
    @Column(name = "assas_customer_id", length = 100)
    private String assasCustomerId;

    @Column(name = "assas_subscription_id", length = 100)
    private String assasSubscriptionId;

    // Upgrade/Downgrade (legado - pro-rata)
    @Column(name = "credito_pro_rata", precision = 10, scale = 2)
    private BigDecimal creditoProRata;

    @Column(name = "cobranca_upgrade_assas_id", length = 100)
    private String cobrancaUpgradeAssasId;

    @Column(name = "plano_anterior_codigo", length = 50)
    private String planoAnteriorCodigo;

    // Troca de plano agendada (efetivada na virada do ciclo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_agendado_id")
    private PlanoBellory planoAgendado;

    @Enumerated(EnumType.STRING)
    @Column(name = "ciclo_agendado", length = 10)
    private CicloCobranca cicloAgendado;

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
        return switch (status) {
            case TRIAL -> isTrialExpirado();
            case AGUARDANDO_PAGAMENTO -> true;
            case UPGRADE_PENDENTE -> false;
            case DOWNGRADE_AGENDADO -> false;
            case ATIVA -> false;
            case VENCIDA, SUSPENSA -> true;
            case CANCELADA -> dtProximoVencimento == null || LocalDateTime.now().isAfter(dtProximoVencimento);
        };
    }

    public boolean temTrocaAgendada() {
        return planoAgendado != null;
    }

    public boolean isPlanoGratuito() {
        return planoBellory != null
                && planoBellory.getPrecoMensal() != null
                && planoBellory.getPrecoMensal().compareTo(java.math.BigDecimal.ZERO) == 0;
    }
}
