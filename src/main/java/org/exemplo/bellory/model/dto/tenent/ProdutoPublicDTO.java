package org.exemplo.bellory.model.dto.tenent;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProdutoPublicDTO {
    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private BigDecimal precoComDesconto;
    private Integer descontoPercentual;
    private List<String> imagens;
    private String categoria;
    private Boolean emEstoque;
    private BigDecimal avaliacao;
    private Integer totalAvaliacoes;
}
