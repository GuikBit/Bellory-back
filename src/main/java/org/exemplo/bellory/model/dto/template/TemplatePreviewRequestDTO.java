package org.exemplo.bellory.model.dto.template;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplatePreviewRequestDTO {

    private Map<String, String> variaveis;
}
