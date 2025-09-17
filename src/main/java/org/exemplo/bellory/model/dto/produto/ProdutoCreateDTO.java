package org.exemplo.bellory.model.dto.produto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ProdutoCreateDTO {

    @NotNull(message = "ID da organização é obrigatório")
    private Long organizacaoId;

    @NotBlank(message = "Nome do produto é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    private String nome;

    @NotNull(message = "ID da categoria é obrigatório")
    private Long categoriaId;

    @Size(max = 100, message = "Gênero deve ter no máximo 100 caracteres")
    private String genero;

    private Boolean destaque = false;

    @Size(max = 5000, message = "Descrição deve ter no máximo 5000 caracteres")
    private String descricao;

    @Size(max = 50, message = "Código de barras deve ter no máximo 50 caracteres")
    private String codigoBarras;

    @Size(max = 50, message = "Código interno deve ter no máximo 50 caracteres")
    private String codigoInterno;

    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "Preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal preco;

    @DecimalMin(value = "0.00", message = "Preço de custo deve ser maior ou igual a zero")
    @Digits(integer = 8, fraction = 2, message = "Preço de custo deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal precoCusto;

    @Min(value = 0, message = "Quantidade em estoque deve ser maior ou igual a zero")
    private Integer quantidadeEstoque = 0;

    @Min(value = 0, message = "Estoque mínimo deve ser maior ou igual a zero")
    private Integer estoqueMinimo = 0;

    @Size(max = 10, message = "Unidade deve ter no máximo 10 caracteres")
    private String unidade;

    @Size(max = 100, message = "Marca deve ter no máximo 100 caracteres")
    private String marca;

    @Size(max = 50, message = "Modelo deve ter no máximo 50 caracteres")
    private String modelo;

    @DecimalMin(value = "0.000", message = "Peso deve ser maior ou igual a zero")
    @Digits(integer = 5, fraction = 3, message = "Peso deve ter no máximo 5 dígitos inteiros e 3 decimais")
    private BigDecimal peso;

    @Min(value = 0, message = "Desconto percentual deve ser maior ou igual a zero")
    @Max(value = 100, message = "Desconto percentual deve ser menor ou igual a 100")
    private Integer descontoPercentual;

    private List<String> urlsImagens;
    private List<String> ingredientes;
    private List<String> comoUsar;
    private Map<String, String> especificacoes;

    private String usuarioCriacao;
}
