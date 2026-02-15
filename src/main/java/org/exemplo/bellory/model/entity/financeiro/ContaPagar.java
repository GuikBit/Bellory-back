package org.exemplo.bellory.model.entity.financeiro;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "conta_pagar", schema = "app", indexes = {
    @Index(name = "idx_cp_conta_bancaria", columnList = "conta_bancaria_id"),
    @Index(name = "idx_cp_org_vencimento", columnList = "organizacao_id, dt_vencimento"),
    @Index(name = "idx_cp_org_status", columnList = "organizacao_id, status"),
    @Index(name = "idx_cp_fornecedor", columnList = "fornecedor")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContaPagar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_financeira_id")
    private CategoriaFinanceira categoriaFinanceira;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_custo_id")
    private CentroCusto centroCusto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_bancaria_id")
    private ContaBancaria contaBancaria;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(length = 200)
    private String fornecedor;

    @Column(length = 50)
    private String documento;

    @Column(name = "numero_nota", length = 50)
    private String numeroNota;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "valor_pago", precision = 14, scale = 2)
    private BigDecimal valorPago = BigDecimal.ZERO;

    @Column(name = "valor_desconto", precision = 14, scale = 2)
    private BigDecimal valorDesconto = BigDecimal.ZERO;

    @Column(name = "valor_juros", precision = 14, scale = 2)
    private BigDecimal valorJuros = BigDecimal.ZERO;

    @Column(name = "valor_multa", precision = 14, scale = 2)
    private BigDecimal valorMulta = BigDecimal.ZERO;

    @Column(name = "dt_emissao")
    private LocalDate dtEmissao;

    @Column(name = "dt_vencimento", nullable = false)
    private LocalDate dtVencimento;

    @Column(name = "dt_pagamento")
    private LocalDate dtPagamento;

    @Column(name = "dt_competencia")
    private LocalDate dtCompetencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusContaPagar status = StatusContaPagar.PENDENTE;

    @Column(name = "forma_pagamento", length = 30)
    private String formaPagamento;

    @Column(nullable = false)
    private Boolean recorrente = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Periodicidade periodicidade;

    @Column(name = "parcela_atual")
    private Integer parcelaAtual;

    @Column(name = "total_parcelas")
    private Integer totalParcelas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_pagar_origem_id")
    @JsonIgnore
    private ContaPagar contaPagarOrigem;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    // === ENUMS ===
    public enum StatusContaPagar {
        PENDENTE("Pendente"),
        PAGA("Paga"),
        VENCIDA("Vencida"),
        CANCELADA("Cancelada"),
        PARCIALMENTE_PAGA("Parcialmente Paga");

        private final String descricao;

        StatusContaPagar(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum Periodicidade {
        SEMANAL("Semanal"),
        QUINZENAL("Quinzenal"),
        MENSAL("Mensal"),
        BIMESTRAL("Bimestral"),
        TRIMESTRAL("Trimestral"),
        SEMESTRAL("Semestral"),
        ANUAL("Anual");

        private final String descricao;

        Periodicidade(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // === LIFECYCLE ===
    @PrePersist
    public void prePersist() {
        if (this.dtCriacao == null) {
            this.dtCriacao = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = StatusContaPagar.PENDENTE;
        }
        if (this.valorPago == null) {
            this.valorPago = BigDecimal.ZERO;
        }
        if (this.valorDesconto == null) {
            this.valorDesconto = BigDecimal.ZERO;
        }
        if (this.valorJuros == null) {
            this.valorJuros = BigDecimal.ZERO;
        }
        if (this.valorMulta == null) {
            this.valorMulta = BigDecimal.ZERO;
        }
        if (this.recorrente == null) {
            this.recorrente = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }

    // === MÉTODOS DE CÁLCULO ===
    public BigDecimal getValorTotal() {
        return this.valor
                .add(this.valorJuros)
                .add(this.valorMulta)
                .subtract(this.valorDesconto);
    }

    public BigDecimal getValorRestante() {
        return getValorTotal().subtract(this.valorPago);
    }

    // === MÉTODOS DE STATUS ===
    public void pagar(BigDecimal valorPagamento, LocalDate dataPagamento) {
        this.valorPago = this.valorPago.add(valorPagamento);
        this.dtPagamento = dataPagamento;

        if (this.valorPago.compareTo(getValorTotal()) >= 0) {
            this.status = StatusContaPagar.PAGA;
        } else {
            this.status = StatusContaPagar.PARCIALMENTE_PAGA;
        }
    }

    public void cancelar() {
        this.status = StatusContaPagar.CANCELADA;
    }

    // === MÉTODOS DE VERIFICAÇÃO ===
    public boolean isPaga() {
        return this.status == StatusContaPagar.PAGA;
    }

    public boolean isPendente() {
        return this.status == StatusContaPagar.PENDENTE ||
                this.status == StatusContaPagar.PARCIALMENTE_PAGA;
    }

    public boolean isVencida() {
        return this.dtVencimento != null &&
                this.dtVencimento.isBefore(LocalDate.now()) &&
                !isPaga() && this.status != StatusContaPagar.CANCELADA;
    }

    public int getDiasAtraso() {
        if (this.dtVencimento == null || !isVencida()) return 0;
        return (int) ChronoUnit.DAYS.between(this.dtVencimento, LocalDate.now());
    }

    public boolean isParcelada() {
        return this.totalParcelas != null && this.totalParcelas > 1;
    }
}
