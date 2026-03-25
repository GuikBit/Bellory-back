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
}
