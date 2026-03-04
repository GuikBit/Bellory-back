package org.exemplo.bellory.model.dto.assinatura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAssinaturaFiltroDTO {
    private String status;
    private String planoCodigo;
}
