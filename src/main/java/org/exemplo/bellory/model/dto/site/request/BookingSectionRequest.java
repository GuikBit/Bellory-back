package org.exemplo.bellory.model.dto.site.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSectionRequest {

    private String sectionTitle;
    private String sectionSubtitle;
    private Boolean enabled;
    private String backgroundColor;
    private String backgroundPattern;
    private Double patternOpacity;
}
