package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.tenent.ProdutoPublicDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO com detalhes completos de um produto para a página dedicada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProdutoDetalhadoDTO {

    private Long id;
    private String nome;
    private String descricao;
    private String descricaoCompleta;

    // ==================== PREÇOS ====================
    private BigDecimal preco;
    private BigDecimal precoComDesconto;
    private Integer descontoPercentual;
    private BigDecimal precoCusto;

    // ==================== IMAGENS ====================
    private List<String> imagens;
    private String imagemPrincipal;

    // ==================== CATEGORIA ====================
    private Long categoriaId;
    private String categoriaNome;

    // ==================== ESTOQUE ====================
    private Boolean emEstoque;
    private Integer quantidadeEstoque;
    private String unidade;

    // ==================== AVALIAÇÕES ====================
    private BigDecimal avaliacao;
    private Integer totalAvaliacoes;

    // ==================== DETALHES DO PRODUTO ====================
    private String marca;
    private String modelo;
    private BigDecimal peso;
    private String codigoBarras;
    private String codigoInterno;

    // ==================== INFORMAÇÕES ADICIONAIS ====================
    private List<String> ingredientes;
    private List<String> comoUsar;
    private Map<String, String> especificacoes;

    // ==================== PRODUTOS RELACIONADOS ====================
    private List<ProdutoPublicDTO> produtosRelacionados;

    // ==================== SEO ====================
    private SeoDataDTO seo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeoDataDTO {
        private String title;
        private String description;
        private String keywords;
        private String canonicalUrl;
        private String ogImage;
    }
}
