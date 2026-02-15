package org.exemplo.bellory.model.dto.landingpage.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.landingpage.LandingPageSectionDTO;

import java.util.Map;

/**
 * Request para adicionar uma nova seção à Landing Page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddSectionRequest {

    /**
     * Tipo da seção: HEADER, HERO, ABOUT, SERVICES, etc.
     */
    @NotBlank(message = "Tipo da seção é obrigatório")
    private String tipo;

    /**
     * Nome da seção para exibição no editor.
     */
    private String nome;

    /**
     * Template/variante a usar.
     */
    private String template;

    /**
     * Posição onde inserir (null = final).
     */
    private Integer posicao;

    /**
     * Se deve copiar de um template existente.
     */
    private Long templateSectionId;

    /**
     * Conteúdo inicial (se não usar template).
     */
    private LandingPageSectionDTO.SectionContentDTO content;

    /**
     * Estilos iniciais.
     */
    private LandingPageSectionDTO.SectionStylesDTO styles;

    /**
     * Configurações iniciais.
     */
    private Map<String, Object> settings;
}
