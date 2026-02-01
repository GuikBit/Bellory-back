package org.exemplo.bellory.model.dto.questionario;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcaoSelecionadaDTO {
    private Long id;
    private String texto;
    private String valor;
}
