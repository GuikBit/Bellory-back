package org.exemplo.bellory.model.dto.questionario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.questionario.enums.TipoTemplateTermo;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateTermoDTO {

    private TipoTemplateTermo id;
    private String nome;
    private String descricao;
    private String conteudo;
    private List<String> variaveis;
    private boolean editavel;
}
