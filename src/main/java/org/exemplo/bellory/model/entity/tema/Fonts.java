package org.exemplo.bellory.model.entity.tema;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Fonts {
    private String heading;
    private String body;
    private String mono;
}
