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

    /**
     * Dados adicionais para o hero template
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
}
