package org.exemplo.bellory.model.entity.financeiro;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "conta_bancaria", schema = "app", indexes = {
    @Index(name = "idx_cb_org_ativo", columnList = "organizacao_id, ativo")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conta", nullable = false, length = 30)
    private TipoConta tipoConta;

    @Column(length = 100)
    private String banco;

    @Column(length = 20)
    private String agencia;

    @Column(name = "numero_conta", length = 30)
    private String numeroConta;

    @Column(name = "saldo_inicial", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(name = "saldo_atual", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoAtual = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean principal = false;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(length = 7)
    private String cor;

    @Column(length = 50)
    private String icone;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    // === ENUMS ===
    public enum TipoConta {
        CONTA_CORRENTE("Conta Corrente"),
        POUPANCA("Poupança"),
        CAIXA("Caixa"),
        CARTEIRA_DIGITAL("Carteira Digital");

        private final String descricao;

        TipoConta(String descricao) {
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
        if (this.saldoInicial == null) {
            this.saldoInicial = BigDecimal.ZERO;
        }
        if (this.saldoAtual == null) {
            this.saldoAtual = this.saldoInicial;
        }
        if (this.ativo == null) {
            this.ativo = true;
        }
        if (this.principal == null) {
            this.principal = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }

    // === MÉTODOS DE CONVENIÊNCIA ===
    public void creditar(BigDecimal valor) {
        this.saldoAtual = this.saldoAtual.add(valor);
    }

    public void debitar(BigDecimal valor) {
        this.saldoAtual = this.saldoAtual.subtract(valor);
    }

    public boolean isSaldoSuficiente(BigDecimal valor) {
        return this.saldoAtual.compareTo(valor) >= 0;
    }
}
