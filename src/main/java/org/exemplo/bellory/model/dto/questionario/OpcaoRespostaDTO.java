package org.exemplo.bellory.model.dto.questionario;

import lombok.*;
import org.exemplo.bellory.model.entity.questionario.OpcaoResposta;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcaoRespostaDTO {

    private Long id;
    private String texto;
    private String valor;
    private Integer ordem;

    public OpcaoRespostaDTO(OpcaoResposta entity) {
        this.id = entity.getId();
        this.texto = entity.getTexto();
        this.valor = entity.getValor();
        this.ordem = entity.getOrdem();
    }
}
