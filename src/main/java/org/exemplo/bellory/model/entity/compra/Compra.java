package org.exemplo.bellory.model.entity.compra;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compra", schema = "app")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToOne(mappedBy = "compra")
    @JsonIgnore
    private Cobranca cobranca;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CompraProduto> itens = new ArrayList<>();

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "valor_desconto", precision = 10, scale = 2)
    private BigDecimal valorDesconto = BigDecimal.ZERO;

    @Column(name = "valor_final", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_compra", nullable = false, length = 50)
    private StatusCompra statusCompra;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_compra", nullable = false, length = 30)
    private TipoCompra tipoCompra = TipoCompra.BALCAO;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "numero_pedido", unique = true, length = 20)
    private String numeroPedido;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(name = "dt_finalizacao")
    private LocalDateTime dtFinalizacao;

    // === ENUMS ===
    public enum StatusCompra {
        CARRINHO("Carrinho"),
        AGUARDANDO_PAGAMENTO("Aguardando Pagamento"),
        PAGO("Pago"),
        PROCESSANDO("Processando"),
        PRONTO("Pronto para Retirada"),
        ENTREGUE("Entregue"),
        CANCELADA("Cancelada");

        private final String descricao;

        StatusCompra(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum TipoCompra {
        ONLINE("Online"),
        BALCAO("Balcão"),
        TELEFONE("Telefone");

        private final String descricao;

        TipoCompra(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // === MÉTODOS DE CONVENIÊNCIA PARA ITENS ===
    public void adicionarItem(CompraProduto item) {
        item.setCompra(this);
        this.itens.add(item);
        recalcularValores();
    }

    public void removerItem(CompraProduto item) {
        item.setCompra(null);
        this.itens.remove(item);
        recalcularValores();
    }

    // === MÉTODOS DE CÁLCULO ===
    public void recalcularValores() {
        this.valorTotal = this.itens.stream()
                .map(CompraProduto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.valorFinal = this.valorTotal.subtract(this.valorDesconto != null ? this.valorDesconto : BigDecimal.ZERO);
    }

    public void aplicarDesconto(BigDecimal desconto) {
        this.valorDesconto = desconto;
        recalcularValores();
    }

    // === MÉTODOS DE STATUS ===
    public void finalizarCompra() {
        this.statusCompra = StatusCompra.AGUARDANDO_PAGAMENTO;
        this.dtFinalizacao = LocalDateTime.now();
        this.dtAtualizacao = LocalDateTime.now();
    }

    public void marcarComoPaga() {
        this.statusCompra = StatusCompra.PAGO;
        this.dtAtualizacao = LocalDateTime.now();
    }

    public void marcarComoEntregue() {
        this.statusCompra = StatusCompra.ENTREGUE;
        this.dtAtualizacao = LocalDateTime.now();
    }

    public void cancelarCompra() {
        this.statusCompra = StatusCompra.CANCELADA;
        this.dtAtualizacao = LocalDateTime.now();
    }

    // === MÉTODOS DE VERIFICAÇÃO ===
    public boolean isCarrinho() {
        return this.statusCompra == StatusCompra.CARRINHO;
    }

    public boolean isFinalizada() {
        return this.statusCompra != StatusCompra.CARRINHO && this.statusCompra != StatusCompra.CANCELADA;
    }

    public boolean isPaga() {
        return this.statusCompra == StatusCompra.PAGO ||
                this.statusCompra == StatusCompra.PROCESSANDO ||
                this.statusCompra == StatusCompra.PRONTO ||
                this.statusCompra == StatusCompra.ENTREGUE;
    }

    public int getQuantidadeTotalItens() {
        return this.itens.stream()
                .mapToInt(CompraProduto::getQuantidade)
                .sum();
    }

    @PrePersist
    public void prePersist() {
        if (this.dtCriacao == null) {
            this.dtCriacao = LocalDateTime.now();
        }
        if (this.statusCompra == null) {
            this.statusCompra = StatusCompra.CARRINHO;
        }
        if (this.numeroPedido == null) {
            gerarNumeroPedido();
        }
        if (this.valorDesconto == null) {
            this.valorDesconto = BigDecimal.ZERO;
        }
        recalcularValores();
    }

    @PreUpdate
    public void preUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
        recalcularValores();
    }

    private void gerarNumeroPedido() {
        // Gera número do pedido no formato: CP + timestamp + clienteId
        this.numeroPedido = "CP" + System.currentTimeMillis() +
                (this.cliente != null ? this.cliente.getId() : "");
    }
}
