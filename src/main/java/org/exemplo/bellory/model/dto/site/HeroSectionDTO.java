package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para a seção Hero do site público.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeroSectionDTO {

    /**
     * Tipo do hero: TEMPLATE ou CUSTOM_HTML
     */
    private String type;

    private String title;
    private String subtitle;
    private String backgroundUrl;
    private Double backgroundOverlay;

    /**
     * HTML customizado (quando type = CUSTOM_HTML)
     */
    private String customHtml;

    private List<HeroButtonDTO> buttons;
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
    private StatsConfigDTO statsConfig;

    private String backgroundColor;
    private String backgroundPattern;
    private Double patternOpacity;

    /**
     * Dados adicionais para o hero template (calculados automaticamente)
     */
    private HeroStatsDTO stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeroButtonDTO {
        private String label;
        private String href;
        private String type; // primary, secondary, outline
        private String icon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeroStatsDTO {
        private Integer yearsExperience;
        private Integer happyClients;
        private Integer servicesCount;
        private Integer teamSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsConfigDTO {
        private Boolean show;
        private Integer yearsExperienceManual;
        private Integer happyClientsManual;
        private String yearsExperienceLabel;
        private String happyClientsLabel;
        private String servicesCountLabel;
        private String teamSizeLabel;
    }
}
