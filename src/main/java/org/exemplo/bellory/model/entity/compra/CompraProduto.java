package org.exemplo.bellory.model.entity.compra;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.produto.Produto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compra_produto", schema = "app", indexes = {
    @Index(name = "idx_compra_produto_compra_id", columnList = "compra_id"),
    @Index(name = "idx_compra_produto_produto_id", columnList = "produto_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "desconto_item", precision = 10, scale = 2)
    private BigDecimal descontoItem = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "dt_adicionado", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtAdicionado;

    // === MÉTODOS DE CONVENIÊNCIA ===
    public void calcularSubtotal() {
        BigDecimal subtotalBruto = this.precoUnitario.multiply(BigDecimal.valueOf(this.quantidade));
        this.subtotal = subtotalBruto.subtract(this.descontoItem != null ? this.descontoItem : BigDecimal.ZERO);
    }

    public void aplicarDesconto(BigDecimal desconto) {
        this.descontoItem = desconto;
        calcularSubtotal();
    }

    public void atualizarQuantidade(Integer novaQuantidade) {
        this.quantidade = novaQuantidade;
        calcularSubtotal();
    }

    @PrePersist
    public void prePersist() {
        if (this.dtAdicionado == null) {
            this.dtAdicionado = LocalDateTime.now();
        }
        if (this.descontoItem == null) {
            this.descontoItem = BigDecimal.ZERO;
        }
        calcularSubtotal();
    }

    @PreUpdate
    public void preUpdate() {
        calcularSubtotal();
    }
}
