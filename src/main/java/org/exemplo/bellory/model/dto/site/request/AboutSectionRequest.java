package org.exemplo.bellory.model.dto.site.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.site.AboutSectionDTO;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AboutSectionRequest {

    private String title;
    private String subtitle;
    private String description;
    private String fullDescription;
    private String imageUrl;
    private List<String> galleryImages;
    private String videoUrl;
    private List<AboutSectionDTO.HighlightDTO> highlights;
    private String mission;
    private String vision;
    private String values;
}
