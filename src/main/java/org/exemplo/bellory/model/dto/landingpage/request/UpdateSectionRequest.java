package org.exemplo.bellory.model.dto.landingpage.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.landingpage.ElementDTO;
import org.exemplo.bellory.model.dto.landingpage.LandingPageSectionDTO;

import java.util.List;
import java.util.Map;

/**
 * Request para atualizar uma seção específica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSectionRequest {

    private String nome;
    private String template;
    private Boolean visivel;
    private Integer ordem;

    /**
     * Conteúdo completo da seção.
     */
    private LandingPageSectionDTO.SectionContentDTO content;

    /**
     * Estilos da seção.
     */
    private LandingPageSectionDTO.SectionStylesDTO styles;

    /**
     * Configurações específicas.
     */
    private Map<String, Object> settings;

    /**
     * Animações.
     */
    private LandingPageSectionDTO.AnimationDTO animations;

    /**
     * Regras de visibilidade.
     */
    private LandingPageSectionDTO.VisibilityRulesDTO visibilityRules;

    /**
     * Fonte de dados dinâmicos.
     */
    private LandingPageSectionDTO.DataSourceDTO dataSource;
}
