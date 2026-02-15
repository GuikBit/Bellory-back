package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.BloqueioOrganizacao;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BloqueioOrganizacaoDTO {
    private Long id;
    private String titulo;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String tipo;
    private String descricao;
    private Boolean ativo;
    private String origem;
    private Integer anoReferencia;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;

    public BloqueioOrganizacaoDTO(BloqueioOrganizacao entity) {
        this.id = entity.getId();
        this.titulo = entity.getTitulo();
        this.dataInicio = entity.getDataInicio();
        this.dataFim = entity.getDataFim();
        this.tipo = entity.getTipo().name();
        this.descricao = entity.getDescricao();
        this.ativo = entity.getAtivo();
        this.origem = entity.getOrigem().name();
        this.anoReferencia = entity.getAnoReferencia();
        this.dtCriacao = entity.getDtCriacao();
        this.dtAtualizacao = entity.getDtAtualizacao();
    }
}
