package org.exemplo.bellory.model.dto.tenent;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.tema.TemaDTO;
import org.exemplo.bellory.model.entity.tema.Tema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteConfigDTO {
    private Tema tema;
    private String logoUrl;
    private String faviconUrl;
//    private String theme;              // "light", "dark", "custom"
//    private String primaryColor;       // "#FF5733"
//    private String secondaryColor;
//    private String accentColor;
//    private String fontFamily;
//    private String logoUrl;
//    private String faviconUrl;
//    private String bannerUrl;
//    private String backgroundImageUrl;
//    private Boolean showPrices;
//    private Boolean allowOnlineBooking;
//    private Boolean showTeam;
//    private Boolean showProducts;
//    private Boolean showReviews;
}
