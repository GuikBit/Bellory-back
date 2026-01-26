package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.tenent.CategoriaPublicDTO;
import org.exemplo.bellory.model.dto.tenent.ProdutoPublicDTO;

import java.util.List;

/**
 * DTO para a seção de produtos do site público.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductsSectionDTO {

    private String title;
    private String subtitle;
    private Boolean showPrices;

    private List<ProdutoPublicDTO> produtos;
    private List<CategoriaPublicDTO> categorias;

    /**
     * Total de produtos (útil para paginação)
     */
    private Integer totalProdutos;
}
