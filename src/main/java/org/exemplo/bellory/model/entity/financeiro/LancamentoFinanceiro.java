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

@Entity
@Table(name = "lancamento_financeiro", schema = "app")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LancamentoFinanceiro {

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
    @JoinColumn(name = "conta_bancaria_destino_id")
    private ContaBancaria contaBancariaDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_pagar_id")
    @JsonIgnore
    private ContaPagar contaPagar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_receber_id")
    @JsonIgnore
    private ContaReceber contaReceber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoLancamento tipo;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "dt_lancamento", nullable = false)
    private LocalDate dtLancamento;

    @Column(name = "dt_competencia")
    private LocalDate dtCompetencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusLancamento status = StatusLancamento.EFETIVADO;

    @Column(name = "forma_pagamento", length = 30)
    private String formaPagamento;

    @Column(length = 50)
    private String documento;

    @Column(name = "numero_nota", length = 50)
    private String numeroNota;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    // === ENUMS ===
    public enum TipoLancamento {
        RECEITA("Receita"),
        DESPESA("Despesa"),
        TRANSFERENCIA("Transferência");

        private final String descricao;

        TipoLancamento(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum StatusLancamento {
        EFETIVADO("Efetivado"),
        PENDENTE("Pendente"),
        CANCELADO("Cancelado");

        private final String descricao;

        StatusLancamento(String descricao) {
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
            this.status = StatusLancamento.EFETIVADO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }

    // === MÉTODOS DE CONVENIÊNCIA ===
    public boolean isReceita() {
        return this.tipo == TipoLancamento.RECEITA;
    }

    public boolean isDespesa() {
        return this.tipo == TipoLancamento.DESPESA;
    }

    public boolean isTransferencia() {
        return this.tipo == TipoLancamento.TRANSFERENCIA;
    }

    public boolean isEfetivado() {
        return this.status == StatusLancamento.EFETIVADO;
    }

    public boolean isPendente() {
        return this.status == StatusLancamento.PENDENTE;
    }

    public void efetivar() {
        this.status = StatusLancamento.EFETIVADO;
    }

    public void cancelar() {
        this.status = StatusLancamento.CANCELADO;
    }
}
