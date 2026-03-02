package org.exemplo.bellory.model.entity.assinatura;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobranca_plataforma", schema = "admin",
    indexes = {
        @Index(name = "idx_cobranca_plat_assinatura", columnList = "assinatura_id"),
        @Index(name = "idx_cobranca_plat_organizacao", columnList = "organizacao_id"),
        @Index(name = "idx_cobranca_plat_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CobrancaPlataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id", nullable = false)
    private Assinatura assinatura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "dt_vencimento", nullable = false)
    private LocalDate dtVencimento;

    @Column(name = "dt_pagamento")
    private LocalDateTime dtPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusCobrancaPlataforma status = StatusCobrancaPlataforma.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 20)
    private FormaPagamentoPlataforma formaPagamento;

    // Dados Assas
    @Column(name = "assas_payment_id", length = 100)
    private String assasPaymentId;

    @Column(name = "assas_invoice_url", length = 500)
    private String assasInvoiceUrl;

    @Column(name = "assas_bank_slip_url", length = 500)
    private String assasBankSlipUrl;

    @Column(name = "assas_pix_qr_code", columnDefinition = "TEXT")
    private String assasPixQrCode;

    @Column(name = "assas_pix_copia_cola", columnDefinition = "TEXT")
    private String assasPixCopiaCola;

    @Column(name = "referencia_mes")
    private Integer referenciaMes;

    @Column(name = "referencia_ano")
    private Integer referenciaAno;

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
}
