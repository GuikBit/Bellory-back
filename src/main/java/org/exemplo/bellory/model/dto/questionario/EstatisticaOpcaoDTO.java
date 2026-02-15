package org.exemplo.bellory.model.dto.questionario;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstatisticaOpcaoDTO {
    private Long opcaoId;
    private String opcaoTexto;
    private Long totalSelecoes;
    private Double percentual;
}
