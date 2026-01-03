package org.exemplo.bellory.model.entity.cobranca;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.compra.Compra;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.pagamento.Pagamento;
import org.exemplo.bellory.model.entity.users.Cliente;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cobranca", schema = "app")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id")
    @JsonIgnore
    private Agendamento agendamento;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", referencedColumnName = "id")
    private Compra compra;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "valor_pago", precision = 10, scale = 2)
    private BigDecimal valorPago = BigDecimal.ZERO;

    @Column(name = "valor_pendente", precision = 10, scale = 2)
    private BigDecimal valorPendente;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_cobranca", nullable = false, length = 50)
    private StatusCobranca statusCobranca;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cobranca", nullable = false, length = 30)
    private TipoCobranca tipoCobranca;

    // NOVO: Subtipo específico para agendamentos
    @Enumerated(EnumType.STRING)
    @Column(name = "subtipo_cobranca_agendamento", length = 30)
    private SubtipoCobrancaAgendamento subtipoCobrancaAgendamento;

    @Column(name = "dt_vencimento")
    private LocalDate dtVencimento;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(name = "dt_pagamento_completo")
    private LocalDateTime dtPagamentoCompleto;

    @OneToMany(mappedBy = "cobranca", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("cobranca-pagamentos")
    private List<Pagamento> pagamentos = new ArrayList<>();

    @Column(name = "numero_cobranca", unique = true, length = 20)
    private String numeroCobranca;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "permite_parcelamento")
    private Boolean permiteParcelamento = true;

    @Column(name = "juros_atraso", precision = 5, scale = 2)
    private BigDecimal jurosAtraso = BigDecimal.ZERO;

    @Column(name = "multa_atraso", precision = 10, scale = 2)
    private BigDecimal multaAtraso = BigDecimal.ZERO;

    // NOVO: Percentual do sinal (ex: 30% = 30.00)
    @Column(name = "percentual_sinal", precision = 5, scale = 2)
    private BigDecimal percentualSinal;

    // NOVO: Referência para cobrança relacionada (sinal <-> restante)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cobranca_relacionada_id")
    private Cobranca cobrancaRelacionada;

    // NOVO: ID do pagamento no gateway (Stripe, etc)
    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    // NOVO: ID da intenção de pagamento (Stripe PaymentIntent)
    @Column(name = "gateway_payment_intent_id", length = 100)
    private String gatewayPaymentIntentId;

    // === ENUMS ===
    public enum StatusCobranca {
        PENDENTE("Pendente"),
        PARCIALMENTE_PAGO("Parcialmente Pago"),
        PAGO("Pago"),
        VENCIDA("Vencida"),
        CANCELADA("Cancelada"),
        ESTORNADA("Estornada");

        private final String descricao;

        StatusCobranca(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum TipoCobranca {
        AGENDAMENTO("Agendamento de Serviço"),
        COMPRA("Compra de Produtos"),
        TAXA_ADICIONAL("Taxa Adicional"),
        MULTA("Multa");

        private final String descricao;

        TipoCobranca(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // NOVO: Subtipo para diferenciar sinal e pagamento final
    public enum SubtipoCobrancaAgendamento {
        SINAL("Sinal/Entrada"),
        RESTANTE("Pagamento Final"),
        INTEGRAL("Pagamento Integral");

        private final String descricao;

        SubtipoCobrancaAgendamento(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // === MÉTODOS DE CONVENIÊNCIA PARA PAGAMENTOS ===
    public void adicionarPagamento(Pagamento pagamento) {
        pagamento.setCobranca(this);
        pagamento.setCliente(this.cliente);
        this.pagamentos.add(pagamento);
        recalcularValores();
    }

    public void removerPagamento(Pagamento pagamento) {
        pagamento.setCobranca(null);
        this.pagamentos.remove(pagamento);
        recalcularValores();
    }

    // === MÉTODOS DE CÁLCULO ===
    public void recalcularValores() {
        this.valorPago = this.pagamentos.stream()
                .filter(p -> p.getStatusPagamento() == Pagamento.StatusPagamento.CONFIRMADO)
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.valorPendente = this.valor.subtract(this.valorPago);

        atualizarStatus();
    }

    public void aplicarJurosEMulta() {
        if (isVencida() && this.jurosAtraso.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal juros = this.valor.multiply(this.jurosAtraso).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            this.valor = this.valor.add(juros).add(this.multaAtraso);
            recalcularValores();
        }
    }

    // === MÉTODOS DE STATUS ===
    private void atualizarStatus() {
        if (this.valorPago.compareTo(BigDecimal.ZERO) == 0) {
            if (isVencida()) {
                this.statusCobranca = StatusCobranca.VENCIDA;
            } else {
                this.statusCobranca = StatusCobranca.PENDENTE;
            }
        } else if (this.valorPago.compareTo(this.valor) >= 0) {
            this.statusCobranca = StatusCobranca.PAGO;
            if (this.dtPagamentoCompleto == null) {
                this.dtPagamentoCompleto = LocalDateTime.now();
            }
        } else {
            this.statusCobranca = StatusCobranca.PARCIALMENTE_PAGO;
        }
    }

    public void marcarComoPaga() {
        this.statusCobranca = StatusCobranca.PAGO;
        this.dtPagamentoCompleto = LocalDateTime.now();
        this.dtAtualizacao = LocalDateTime.now();
    }

    public void cancelar() {
        this.statusCobranca = StatusCobranca.CANCELADA;
        this.dtAtualizacao = LocalDateTime.now();
    }

    public void estornar() {
        this.statusCobranca = StatusCobranca.ESTORNADA;
        this.valorPago = BigDecimal.ZERO;
        this.valorPendente = this.valor;
        this.dtAtualizacao = LocalDateTime.now();
    }

    // === MÉTODOS DE VERIFICAÇÃO ===
    public boolean isPaga() {
        return this.statusCobranca == StatusCobranca.PAGO;
    }

    public boolean isPendente() {
        return this.statusCobranca == StatusCobranca.PENDENTE ||
                this.statusCobranca == StatusCobranca.PARCIALMENTE_PAGO;
    }

    public boolean isVencida() {
        return this.dtVencimento != null &&
                this.dtVencimento.isBefore(LocalDate.now()) &&
                !isPaga();
    }

    public boolean isCancelada() {
        return this.statusCobranca == StatusCobranca.CANCELADA;
    }

    public boolean permiteNovoPagamento() {
        return this.statusCobranca == StatusCobranca.PENDENTE ||
                this.statusCobranca == StatusCobranca.PARCIALMENTE_PAGO ||
                this.statusCobranca == StatusCobranca.VENCIDA;
    }

    // NOVO: Verifica se é cobrança de sinal
    public boolean isSinal() {
        return this.subtipoCobrancaAgendamento == SubtipoCobrancaAgendamento.SINAL;
    }

    // NOVO: Verifica se é cobrança do restante
    public boolean isRestante() {
        return this.subtipoCobrancaAgendamento == SubtipoCobrancaAgendamento.RESTANTE;
    }

    // NOVO: Verifica se é cobrança integral
    public boolean isIntegral() {
        return this.subtipoCobrancaAgendamento == SubtipoCobrancaAgendamento.INTEGRAL;
    }

    public BigDecimal getValorRestante() {
        return this.valor.subtract(this.valorPago);
    }

    public String getDescricaoTransacao() {
        if (this.agendamento != null) {
            String desc = "Agendamento - " + getDescricaoServicos();
            if (this.subtipoCobrancaAgendamento != null) {
                desc += " (" + this.subtipoCobrancaAgendamento.getDescricao() + ")";
            }
            return desc;
        } else if (this.compra != null) {
            return "Compra #" + this.compra.getNumeroPedido();
        }
        return "Cobrança #" + this.numeroCobranca;
    }

    private String getDescricaoServicos() {
        if (this.agendamento != null && this.agendamento.getServicos() != null) {
            return this.agendamento.getServicos().stream()
                    .map(servico -> servico.getNome())
                    .reduce((s1, s2) -> s1 + ", " + s2)
                    .orElse("Serviços");
        }
        return "Serviços";
    }

    public int getDiasAtraso() {
        if (this.dtVencimento == null) return 0;

        LocalDate hoje = LocalDate.now();
        if (this.dtVencimento.isBefore(hoje)) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(this.dtVencimento, hoje);
        }
        return 0;
    }

    @PrePersist
    public void prePersist() {
        if (this.dtCriacao == null) {
            this.dtCriacao = LocalDateTime.now();
        }
        if (this.statusCobranca == null) {
            this.statusCobranca = StatusCobranca.PENDENTE;
        }
        if (this.numeroCobranca == null) {
            gerarNumeroCobranca();
        }
        if (this.valorPago == null) {
            this.valorPago = BigDecimal.ZERO;
        }
        if (this.jurosAtraso == null) {
            this.jurosAtraso = BigDecimal.ZERO;
        }
        if (this.multaAtraso == null) {
            this.multaAtraso = BigDecimal.ZERO;
        }

        // Determinar tipo de cobrança
        if (this.tipoCobranca == null) {
            if (this.agendamento != null) {
                this.tipoCobranca = TipoCobranca.AGENDAMENTO;
            } else if (this.compra != null) {
                this.tipoCobranca = TipoCobranca.COMPRA;
            }
        }

        this.valorPendente = this.valor;
    }

    @PreUpdate
    public void preUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
        recalcularValores();
    }

    private void gerarNumeroCobranca() {
        // Gera número da cobrança no formato: COB + timestamp + clienteId + subtipo
        String sufixo = "";
        if (this.subtipoCobrancaAgendamento != null) {
            switch (this.subtipoCobrancaAgendamento) {
                case SINAL:
                    sufixo = "S";
                    break;
                case RESTANTE:
                    sufixo = "R";
                    break;
                case INTEGRAL:
                    sufixo = "I";
                    break;
            }
        }
        this.numeroCobranca = "COB" + System.currentTimeMillis() +
                (this.cliente != null ? this.cliente.getId() : "") + sufixo;
    }
}
