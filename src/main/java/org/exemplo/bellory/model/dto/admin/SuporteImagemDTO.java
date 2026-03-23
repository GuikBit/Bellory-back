package org.exemplo.bellory.model.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuporteImagemDTO {
    private String nome;
    private String url;
    private String relativePath;
    private String pasta;
    private Long tamanho;
}
