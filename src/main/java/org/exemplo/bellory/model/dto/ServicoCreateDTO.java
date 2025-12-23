package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ServicoCreateDTO {
    private Long organizacaoId;
    private String nome;
    private Long categoriaId;
    private String genero;
    private String descricao;
    private Integer tempoEstimadoMinutos;
    private BigDecimal preco;
    private Integer desconto;
    private List<String> urlsImagens;
    private List<String> produtos;
    private boolean home;
    private boolean avaliacao;
    private boolean ativo;
}
