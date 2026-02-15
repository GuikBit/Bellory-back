package org.exemplo.bellory.model.dto.questionario;

import lombok.*;
import org.exemplo.bellory.model.entity.questionario.Questionario;
import org.exemplo.bellory.model.entity.questionario.enums.TipoQuestionario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionarioDTO {

    private Long id;
    private Long organizacaoId;
    private String titulo;
    private String descricao;
    private TipoQuestionario tipo;
    private List<PerguntaDTO> perguntas;
    private Boolean ativo;
    private Boolean obrigatorio;
    private Boolean anonimo;
    private String urlImagem;
    private String corTema;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;
    private Long totalRespostas;

    public QuestionarioDTO(Questionario entity) {
        this.id = entity.getId();
        this.organizacaoId = entity.getOrganizacao() != null ? entity.getOrganizacao().getId() : null;
        this.titulo = entity.getTitulo();
        this.descricao = entity.getDescricao();
        this.tipo = entity.getTipo();
        this.ativo = entity.getAtivo();
        this.obrigatorio = entity.getObrigatorio();
        this.anonimo = entity.getAnonimo();
        this.urlImagem = entity.getUrlImagem();
        this.corTema = entity.getCorTema();
        this.dtCriacao = entity.getDtCriacao();
        this.dtAtualizacao = entity.getDtAtualizacao();
        this.totalRespostas = entity.getTotalRespostas();

        if (entity.getPerguntas() != null && !entity.getPerguntas().isEmpty()) {
            this.perguntas = entity.getPerguntas().stream()
                    .map(PerguntaDTO::new)
                    .collect(Collectors.toList());
        }
    }
}
