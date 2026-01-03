package org.exemplo.bellory.model.entity.pagamento;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // NOVO: Relacionamento com Organização
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    // RELACIONAMENTO COM CARTÃO (OPCIONAL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_credito_id")
    private CartaoCredito cartaoCredito;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    // ATUALIZADO: Campo renomeado para dt_pagamento
    @Column(name = "dt_pagamento")
    private LocalDateTime dtPagamento;

    // NOVO: Forma de pagamento (compatível com o sistema antigo)
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 50)
    private FormaPagamento formaPagamento;

    // MANTIDO: Método de pagamento (para compatibilidade)
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento", length = 50)
    private MetodoPagamento metodoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    private StatusPagamento statusPagamento;

    // NOVO: Número da transação único
    @Column(name = "numero_transacao", unique = true, length = 50)
    private String numeroTransacao;

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

    // NOVO: Forma de pagamento (simplificada)
    public enum FormaPagamento {
        DINHEIRO("Dinheiro"),
        CARTAO_CREDITO("Cartão de Crédito"),
        CARTAO_DEBITO("Cartão de Débito"),
        PIX("PIX"),
        TRANSFERENCIA("Transferência Bancária"),
        BOLETO("Boleto Bancário"),
        CHEQUE("Cheque");

        private final String descricao;

        FormaPagamento(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

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
        this.dtPagamento = LocalDateTime.now();
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
        if (this.numeroTransacao == null) {
            gerarNumeroTransacao();
        }

        // Sincronizar formaPagamento com metodoPagamento se não estiver definido
        if (this.formaPagamento == null && this.metodoPagamento != null) {
            this.formaPagamento = converterMetodoParaForma(this.metodoPagamento);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }

    private void gerarNumeroTransacao() {
        this.numeroTransacao = "PAG" + System.currentTimeMillis() +
                (this.cliente != null ? this.cliente.getId() : "");
    }

    // Método auxiliar para converter MetodoPagamento para FormaPagamento
    private FormaPagamento converterMetodoParaForma(MetodoPagamento metodo) {
        return switch (metodo) {
            case CARTAO_CREDITO -> FormaPagamento.CARTAO_CREDITO;
            case CARTAO_DEBITO -> FormaPagamento.CARTAO_DEBITO;
            case DINHEIRO -> FormaPagamento.DINHEIRO;
            case PIX -> FormaPagamento.PIX;
            case TRANSFERENCIA_BANCARIA -> FormaPagamento.TRANSFERENCIA;
            case BOLETO -> FormaPagamento.BOLETO;
        };
    }
}
