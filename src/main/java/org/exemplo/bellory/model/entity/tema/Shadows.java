package org.exemplo.bellory.model.entity.tema;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Shadows {
    private String base;
    private String md;
    private String lg;
    private String primaryGlow;
    private String accentGlow;
}
