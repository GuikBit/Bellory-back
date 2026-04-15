package org.exemplo.bellory.model.dto.site.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.site.HomePageDTO;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralSettingsRequest {

    private List<String> homeSectionsOrder;
    private String customCss;
    private String customJs;
    private List<HomePageDTO.ExternalScriptDTO> externalScripts;
    private Boolean active;
}
