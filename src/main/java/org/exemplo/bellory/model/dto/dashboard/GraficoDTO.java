package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraficoDTO {
    private String tipo; // "linha", "barra", "pizza"
    private String titulo;
    private List<String> labels;
    private List<SerieGraficoDTO> series;
}
