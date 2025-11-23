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
public class RedesSociaisDTO {
    private String instagram;
    private String facebook;
    private String twitter;
    private String tiktok;
    private String youtube;
    private String linkedin;
    private String whatsapp;
    private String website;
}
