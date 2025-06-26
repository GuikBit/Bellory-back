package org.exemplo.bellory.model.entity.compra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.exemplo.bellory.model.entity.produto.Produto;

import java.math.BigDecimal;

@Entity
@Table(name = "compra_produto")
public class CompraProduto {

    @EmbeddedId
    private CompraProdutoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("compraId")
    @JoinColumn(name = "compra_id")
    @JsonIgnore
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("produtoId")
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false)
    private int quantidade = 1;

    @Column(name = "preco_unitario_compra", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitarioCompra;

    // Getters e Setters
}