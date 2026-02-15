package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para configuração do header do site público.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeaderConfigDTO {

    private String logoUrl;
    private String logoAlt;
    private List<MenuItemDTO> menuItems;
    private List<ActionButtonDTO> actionButtons;
    private Boolean showPhone;
    private String phoneNumber;
    private Boolean showSocial;
    private Boolean sticky;
    private SocialLinksDTO socialLinks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuItemDTO {
        private String label;
        private String href;
        private Integer order;
        private Boolean external;
        private List<MenuItemDTO> subItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionButtonDTO {
        private String label;
        private String href;
        private String type; // primary, secondary, outline
        private String icon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialLinksDTO {
        private String instagram;
        private String facebook;
        private String whatsapp;
        private String youtube;
        private String linkedin;
        private String tiktok;
    }
}
