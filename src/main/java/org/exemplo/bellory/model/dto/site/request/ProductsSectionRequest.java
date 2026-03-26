package org.exemplo.bellory.model.dto.site.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductsSectionRequest {

    private String sectionTitle;
    private String sectionSubtitle;
    private Boolean showPrices;
    private Integer featuredLimit;
    private String layout;
    private String cardStyle;
    private Integer columns;
    private Integer cardImageHeight;
    private Boolean showRating;
    private Boolean showCategory;
    private Boolean showDescription;
    private Boolean showDiscount;
    private Boolean showStock;
    private Boolean showAddToCart;
    private String hoverEffect;
    private String badgeStyle;
    private Boolean autoPlay;
    private Integer autoPlaySpeed;
    private String backgroundColor;
    private String backgroundPattern;
    private Double patternOpacity;
}
