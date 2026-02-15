package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.tenent.CategoriaPublicDTO;
import org.exemplo.bellory.model.dto.tenent.ServicoPublicDTO;

import java.util.List;

/**
 * DTO para a seção de serviços do site público.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServicesSectionDTO {

    private String title;
    private String subtitle;
    private Boolean showPrices;
    private Boolean showDuration;

    private List<ServicoPublicDTO> servicos;
    private List<CategoriaPublicDTO> categorias;

    /**
     * Total de serviços (útil para paginação)
     */
    private Integer totalServicos;
}
