package org.exemplo.bellory.model.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtualizarStatusConfirmacaoRequest {

    private String dataDesejada; // formato: yyyy-MM-dd
    private List<HorarioDisponivelResponse> horariosDisponiveis;
}
