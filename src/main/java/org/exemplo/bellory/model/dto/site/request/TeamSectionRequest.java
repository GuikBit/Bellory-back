package org.exemplo.bellory.model.dto.site.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamSectionRequest {

    private String sectionTitle;
    private String sectionSubtitle;
    private Boolean showSection;
    private String layout;
    private String cardStyle;
    private String photoShape;
    private Integer photoHeight;
    private Boolean showBio;
    private Boolean showServices;
    private Boolean showSchedule;
    private Boolean carouselAutoPlay;
    private Integer carouselSpeed;
}
