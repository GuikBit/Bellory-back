package org.exemplo.bellory.model.dto.site.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicesSectionRequest {

    private String sectionTitle;
    private String sectionSubtitle;
    private Boolean showPrices;
    private Boolean showDuration;
    private Integer featuredLimit;
    private String cardStyle;
    private Boolean showCategory;
    private Boolean showDescription;
    private Boolean showImage;
    private Boolean showDiscount;
    private Integer cardImageHeight;
    private Boolean showCategoryFilter;
    private Integer columns;
    private String backgroundColor;
    private String backgroundPattern;
    private Double patternOpacity;
}
