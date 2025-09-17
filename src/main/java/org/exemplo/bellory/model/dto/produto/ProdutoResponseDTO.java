package org.exemplo.bellory.model.dto.produto;

import lombok.Data;
import org.exemplo.bellory.model.entity.produto.Produto.StatusProduto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ProdutoResponseDTO {

    private Long id;
    private String nome;
    private String nomeCategoria;
    private Long categoriaId;
    private String genero;
    private BigDecimal avaliacao;
    private Integer totalAvaliacoes;
    private Integer descontoPercentual;
    private Boolean destaque;
    private Boolean ativo;
    private String descricao;
    private String codigoBarras;
    private String codigoInterno;
    private BigDecimal preco;
    private BigDecimal precoCusto;
    private Integer quantidadeEstoque;
    private Integer estoqueMinimo;
    private String unidade;
    private StatusProduto status;
    private String statusDescricao;
    private String marca;
    private String modelo;
    private BigDecimal peso;

    private List<String> urlsImagens;
    private List<String> ingredientes;
    private List<String> comoUsar;
    private Map<String, String> especificacoes;

    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;
    private String usuarioCriacao;
    private String usuarioAtualizacao;

    // Campos calculados
    private BigDecimal margemLucro;
    private Boolean temEstoque;
    private Boolean estoqueAbaixoMinimo;
    private BigDecimal precoComDesconto;
    private String imagemPrincipal;

    // Relacionamentos
    private Long organizacaoId;
    private String nomeOrganizacao;
}
