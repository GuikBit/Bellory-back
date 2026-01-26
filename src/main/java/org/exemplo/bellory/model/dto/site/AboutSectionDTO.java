package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para a seção Sobre do site público.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AboutSectionDTO {

    private String title;
    private String subtitle;
    private String description;
    private String imageUrl;
    private String videoUrl;
    private List<String> galleryImages;
    private List<HighlightDTO> highlights;

    /**
     * Campos adicionais para página "Sobre" completa
     */
    private String fullDescription;
    private String mission;
    private String vision;
    private String values;

    /**
     * Informações da organização
     */
    private OrganizationInfoDTO organizationInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighlightDTO {
        private String icon;
        private String title;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationInfoDTO {
        private String name;
        private String foundedYear;
        private String address;
        private String phone;
        private String email;
        private String whatsapp;
    }
}
