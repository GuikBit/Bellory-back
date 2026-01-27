package org.exemplo.bellory.model.dto.landingpage.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para reordenar seções.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderSectionsRequest {

    /**
     * Lista de IDs de seções na nova ordem.
     */
    @NotEmpty(message = "Lista de seções é obrigatória")
    private List<String> sectionIds;
}
