package org.exemplo.bellory.model.dto.tema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorderRadiusDTO {
    private String small;
    private String medium;
    private String large;
    private String xl;
    private String full;
}
