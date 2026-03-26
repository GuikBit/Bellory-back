package org.exemplo.bellory.model.dto.site.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.site.FooterConfigDTO;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FooterConfigRequest {

    private String description;
    private String logoUrl;
    private List<FooterConfigDTO.LinkSectionDTO> linkSections;
    private String copyrightText;
    private Boolean showMap;
    private Boolean showHours;
    private Boolean showSocial;
    private Boolean showNewsletter;
    private String layout;
    private Integer logoHeight;
    private Boolean showLogo;
    private String socialStyle;
    private String dividerStyle;
    private Boolean showContact;
    private Boolean showBackToTop;
    private String newsletterTitle;
    private String newsletterPlaceholder;
    private Integer columns;
    private Boolean compactHours;
    private String backgroundColor;
    private String backgroundPattern;
    private Double patternOpacity;
}
