package org.exemplo.bellory.model.dto;

import lombok.*;
import org.exemplo.bellory.model.entity.servico.Servico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ServicoDTO {

    private Long id;
    private Long organizacaoId;
    private String nome;
    private String categoria;
    private String genero;
    private String descricao;
    private Integer tempoEstimadoMinutos;
    private BigDecimal preco;
    private List<String> urlsImagens;
    private boolean ativo;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;

    // Construtor que converte a Entidade em DTO
    public ServicoDTO(Servico servico) {
        this.id = servico.getId();
        this.organizacaoId = servico.getOrganizacao().getId();
        this.nome = servico.getNome();
        this.categoria = servico.getCategoria().getLabel();
        this.genero = servico.getGenero();
        this.descricao = servico.getDescricao();
        this.tempoEstimadoMinutos = servico.getTempoEstimadoMinutos();
        this.preco = servico.getPreco();
        this.urlsImagens = servico.getUrlsImagens();
        this.ativo = servico.isAtivo();
        this.dtCriacao = servico.getDtCriacao();
        this.dtAtualizacao = servico.getDtAtualizacao();
    }
}
