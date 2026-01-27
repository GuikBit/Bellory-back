package org.exemplo.bellory.model.dto.landingpage.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.landingpage.LandingPageDTO;

/**
 * Request para criar uma nova Landing Page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLandingPageRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    private String nome;

    @Size(max = 100, message = "Slug deve ter no máximo 100 caracteres")
    private String slug;

    private String descricao;

    /**
     * Tipo: HOME, PROMOCAO, EVENTO, CAMPANHA, CUSTOM
     */
    @Builder.Default
    private String tipo = "HOME";

    /**
     * Se é a página home.
     */
    @Builder.Default
    private Boolean isHome = false;

    /**
     * ID de um template para usar como base.
     */
    private Long templateId;

    /**
     * Configurações globais iniciais.
     */
    private LandingPageDTO.GlobalSettingsDTO globalSettings;

    /**
     * Configurações de SEO iniciais.
     */
    private LandingPageDTO.SeoSettingsDTO seoSettings;
}
