package org.exemplo.bellory.model.entity.financeiro;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.users.Cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "conta_receber", schema = "app")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContaReceber {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cobranca_id")
    @JsonIgnore
    private Cobranca cobranca;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(length = 50)
    private String documento;

    @Column(name = "numero_nota", length = 50)
    private String numeroNota;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "valor_recebido", precision = 14, scale = 2)
    private BigDecimal valorRecebido = BigDecimal.ZERO;

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

    @Column(name = "dt_recebimento")
    private LocalDate dtRecebimento;

    @Column(name = "dt_competencia")
    private LocalDate dtCompetencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusContaReceber status = StatusContaReceber.PENDENTE;

    @Column(name = "forma_pagamento", length = 30)
    private String formaPagamento;

    @Column(nullable = false)
    private Boolean recorrente = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ContaPagar.Periodicidade periodicidade;

    @Column(name = "parcela_atual")
    private Integer parcelaAtual;

    @Column(name = "total_parcelas")
    private Integer totalParcelas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_receber_origem_id")
    @JsonIgnore
    private ContaReceber contaReceberOrigem;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    // === ENUMS ===
    public enum StatusContaReceber {
        PENDENTE("Pendente"),
        RECEBIDA("Recebida"),
        VENCIDA("Vencida"),
        CANCELADA("Cancelada"),
        PARCIALMENTE_RECEBIDA("Parcialmente Recebida");

        private final String descricao;

        StatusContaReceber(String descricao) {
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
            this.status = StatusContaReceber.PENDENTE;
        }
        if (this.valorRecebido == null) {
            this.valorRecebido = BigDecimal.ZERO;
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
        return getValorTotal().subtract(this.valorRecebido);
    }

    // === MÉTODOS DE STATUS ===
    public void receber(BigDecimal valorRecebimento, LocalDate dataRecebimento) {
        this.valorRecebido = this.valorRecebido.add(valorRecebimento);
        this.dtRecebimento = dataRecebimento;

        if (this.valorRecebido.compareTo(getValorTotal()) >= 0) {
            this.status = StatusContaReceber.RECEBIDA;
        } else {
            this.status = StatusContaReceber.PARCIALMENTE_RECEBIDA;
        }
    }

    public void cancelar() {
        this.status = StatusContaReceber.CANCELADA;
    }

    // === MÉTODOS DE VERIFICAÇÃO ===
    public boolean isRecebida() {
        return this.status == StatusContaReceber.RECEBIDA;
    }

    public boolean isPendente() {
        return this.status == StatusContaReceber.PENDENTE ||
                this.status == StatusContaReceber.PARCIALMENTE_RECEBIDA;
    }

    public boolean isVencida() {
        return this.dtVencimento != null &&
                this.dtVencimento.isBefore(LocalDate.now()) &&
                !isRecebida() && this.status != StatusContaReceber.CANCELADA;
    }

    public int getDiasAtraso() {
        if (this.dtVencimento == null || !isVencida()) return 0;
        return (int) ChronoUnit.DAYS.between(this.dtVencimento, LocalDate.now());
    }

    public boolean isParcelada() {
        return this.totalParcelas != null && this.totalParcelas > 1;
    }
}
