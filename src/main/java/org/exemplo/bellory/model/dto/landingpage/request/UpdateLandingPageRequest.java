package org.exemplo.bellory.model.dto.landingpage.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.landingpage.LandingPageDTO;
import org.exemplo.bellory.model.dto.landingpage.LandingPageSectionDTO;

import java.util.List;

/**
 * Request para atualizar uma Landing Page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLandingPageRequest {

    private String nome;
    private String slug;
    private String descricao;
    private String tipo;
    private Boolean isHome;

    /**
     * Lista completa de seções (substituirá as existentes).
     */
    private List<LandingPageSectionDTO> sections;

    /**
     * Configurações globais.
     */
    private LandingPageDTO.GlobalSettingsDTO globalSettings;

    /**
     * Configurações de SEO.
     */
    private LandingPageDTO.SeoSettingsDTO seoSettings;

    /**
     * CSS customizado.
     */
    private String customCss;

    /**
     * JavaScript customizado.
     */
    private String customJs;

    /**
     * URL do favicon.
     */
    private String faviconUrl;

    /**
     * Se deve criar uma nova versão no histórico.
     */
    @Builder.Default
    private Boolean createVersion = true;

    /**
     * Descrição da versão (changelog).
     */
    private String versionDescription;
}
