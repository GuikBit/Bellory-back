package org.exemplo.bellory.model.dto.questionario;

import lombok.*;
import org.exemplo.bellory.model.entity.questionario.Pergunta;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerguntaDTO {

    private Long id;
    private String texto;
    private String descricao;
    private TipoPergunta tipo;
    private Boolean obrigatoria;
    private Integer ordem;
    private List<OpcaoRespostaDTO> opcoes;

    // Campos para tipo ESCALA
    private Integer escalaMin;
    private Integer escalaMax;
    private String labelMin;
    private String labelMax;

    // Campos para validação
    private Integer minCaracteres;
    private Integer maxCaracteres;
    private Double minValor;
    private Double maxValor;

    public PerguntaDTO(Pergunta entity) {
        this.id = entity.getId();
        this.texto = entity.getTexto();
        this.descricao = entity.getDescricao();
        this.tipo = entity.getTipo();
        this.obrigatoria = entity.getObrigatoria();
        this.ordem = entity.getOrdem();
        this.escalaMin = entity.getEscalaMin();
        this.escalaMax = entity.getEscalaMax();
        this.labelMin = entity.getLabelMin();
        this.labelMax = entity.getLabelMax();
        this.minCaracteres = entity.getMinCaracteres();
        this.maxCaracteres = entity.getMaxCaracteres();
        this.minValor = entity.getMinValor();
        this.maxValor = entity.getMaxValor();

        if (entity.getOpcoes() != null && !entity.getOpcoes().isEmpty()) {
            this.opcoes = entity.getOpcoes().stream()
                    .map(OpcaoRespostaDTO::new)
                    .collect(Collectors.toList());
        }
    }
}
