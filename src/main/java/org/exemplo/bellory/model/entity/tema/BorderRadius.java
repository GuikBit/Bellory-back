package org.exemplo.bellory.model.entity.tema;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class BorderRadius {
    private String small;
    private String medium;
    private String large;
    private String xl;
    private String full;
}
