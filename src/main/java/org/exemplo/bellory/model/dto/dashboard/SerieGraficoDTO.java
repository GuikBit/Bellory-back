package org.exemplo.bellory.model.dto.dashboard;


import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerieGraficoDTO {
    private String nome;
    private List<Object> dados;
    private String cor;
}
