package org.exemplo.bellory.model.dto.site.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SitePublicoConfigRequest {

    private HeroSectionRequest hero;
    private HeaderConfigRequest header;
    private AboutSectionRequest about;
    private FooterConfigRequest footer;
    private ServicesSectionRequest services;
    private ProductsSectionRequest products;
    private TeamSectionRequest team;
    private BookingSectionRequest booking;
    private GeneralSettingsRequest general;
}
