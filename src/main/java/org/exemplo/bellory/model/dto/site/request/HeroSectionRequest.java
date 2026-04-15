package org.exemplo.bellory.model.dto.site.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.site.HeroSectionDTO;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeroSectionRequest {

    private String type; // TEMPLATE ou CUSTOM_HTML
    private String title;
    private String subtitle;
    private String backgroundUrl;
    private Double backgroundOverlay;
    private String customHtml;
    private List<HeroSectionDTO.HeroButtonDTO> buttons;
    private Boolean showBookingForm;
    private String contentLayout;
    private String titleSize;
    private String heroHeight;
    private String overlayStyle;
    private String badgeText;
    private String titleHighlight;
    private Boolean showParticles;
    private String videoUrl;
    private String sideImageUrl;
    private HeroSectionDTO.StatsConfigDTO statsConfig;
    private String backgroundColor;
    private String backgroundPattern;
    private Double patternOpacity;
}
