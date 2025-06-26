package org.exemplo.bellory.model.entity.compra;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CompraProdutoId implements Serializable {

    @Column(name = "compra_id")
    private Long compraId;

    @Column(name = "produto_id")
    private Long produtoId;

    // Construtores, hashCode, equals, getters e setters

    public CompraProdutoId() {}

    public CompraProdutoId(Long compraId, Long produtoId) {
        this.compraId = compraId;
        this.produtoId = produtoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompraProdutoId that = (CompraProdutoId) o;
        return Objects.equals(compraId, that.compraId) &&
                Objects.equals(produtoId, that.produtoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(compraId, produtoId);
    }
}