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
}
