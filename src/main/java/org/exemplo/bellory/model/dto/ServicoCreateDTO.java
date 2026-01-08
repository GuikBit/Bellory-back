package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

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
    private List<MultipartFile> imagens;
    private List<String> produtos;
    private boolean home;
    private boolean avaliacao;
    private boolean ativo;
}
