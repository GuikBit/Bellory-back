package org.exemplo.bellory.model.entity.pagamento;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.users.Cliente;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento", schema = "app")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cobranca_id", nullable = false)
    @JsonBackReference("cobranca-pagamentos")
    private Cobranca cobranca;

    // === NOVO RELACIONAMENTO COM CLIENTE ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // === RELACIONAMENTO COM CARTÃO (OPCIONAL) ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_credito_id")
    private CartaoCredito cartaoCredito;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento", nullable = false, length = 50)
    private MetodoPagamento metodoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    private StatusPagamento statusPagamento;

    @Column(name = "transacao_id", length = 255)
    private String transacaoId;

    @Column(name = "comprovante_url", length = 500)
    private String comprovanteUrl;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    // === ENUMS ===
    public enum MetodoPagamento {
        CARTAO_CREDITO("Cartão de Crédito"),
        CARTAO_DEBITO("Cartão de Débito"),
        DINHEIRO("Dinheiro"),
        PIX("PIX"),
        TRANSFERENCIA_BANCARIA("Transferência Bancária"),
        BOLETO("Boleto Bancário");

        private final String descricao;

        MetodoPagamento(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum StatusPagamento {
        PENDENTE("Pendente"),
        PROCESSANDO("Processando"),
        CONFIRMADO("Confirmado"),
        RECUSADO("Recusado"),
        CANCELADO("Cancelado"),
        ESTORNADO("Estornado");

        private final String descricao;

        StatusPagamento(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // === MÉTODOS DE CONVENIÊNCIA ===
    public boolean isPago() {
        return this.statusPagamento == StatusPagamento.CONFIRMADO;
    }

    public boolean isPendente() {
        return this.statusPagamento == StatusPagamento.PENDENTE ||
                this.statusPagamento == StatusPagamento.PROCESSANDO;
    }

    public void confirmarPagamento() {
        this.statusPagamento = StatusPagamento.CONFIRMADO;
        this.dataPagamento = LocalDateTime.now();
        this.dtAtualizacao = LocalDateTime.now();
    }

    public void cancelarPagamento() {
        this.statusPagamento = StatusPagamento.CANCELADO;
        this.dtAtualizacao = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.dtCriacao == null) {
            this.dtCriacao = LocalDateTime.now();
        }
        if (this.statusPagamento == null) {
            this.statusPagamento = StatusPagamento.PENDENTE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }
}
