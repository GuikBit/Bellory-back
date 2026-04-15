package org.exemplo.bellory.model.dto.site.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionConfigRequest {

    private Boolean enabled;
    private String variant;
    private String colorFrom;
    private String colorTo;
    private String accentColor;
    private Boolean flip;
    private String backgroundPattern;
    private Double patternOpacity;
}
