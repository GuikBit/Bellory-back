package org.exemplo.bellory.model.dto.produto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProdutoResumoDTO {
    private Long id;
    private String nome;
    private String marca;
    private BigDecimal preco;
    private Integer quantidadeEstoque;
    private String status;
    private String imagemPrincipal;
    private Boolean destaque;
    private String nomeCategoria;
    private BigDecimal precoComDesconto;
}
