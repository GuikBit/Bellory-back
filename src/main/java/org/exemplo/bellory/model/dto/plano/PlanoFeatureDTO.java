package org.exemplo.bellory.model.dto.plano;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoFeatureDTO {
    private String text;
    private boolean included;
}
