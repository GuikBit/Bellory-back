package org.exemplo.bellory.model.dto.tema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShadowsDTO {
    private String base;
    private String md;
    private String lg;
    private String primaryGlow;
    private String accentGlow;
}
