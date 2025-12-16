package org.exemplo.bellory.model.dto.tenent;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeoMetadataDTO {
    private String title;
    private String description;
    private String keywords;
    private String ogImage;
    private String ogTitle;
    private String ogDescription;
    private String canonicalUrl;
}
