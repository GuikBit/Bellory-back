package org.exemplo.bellory.model.dto.landingpage;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO completo de uma Landing Page.
 * Usado para retornar dados da página e para salvar alterações.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LandingPageDTO {

    private Long id;
    private String slug;
    private String nome;
    private String descricao;
    private String tipo;
    private Boolean isHome;
    private String status;

    /**
     * Lista de seções da página.
     */
    private List<LandingPageSectionDTO> sections;

    /**
     * Configurações globais da página.
     */
    private GlobalSettingsDTO globalSettings;

    /**
     * Configurações de SEO.
     */
    private SeoSettingsDTO seoSettings;

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
     * Versão atual.
     */
    private Integer versao;

    /**
     * Data de publicação.
     */
    private LocalDateTime dtPublicacao;

    /**
     * Data de criação.
     */
    private LocalDateTime dtCriacao;

    /**
     * Data de atualização.
     */
    private LocalDateTime dtAtualizacao;

    // ==================== SUB DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalSettingsDTO {
        private String theme; // light, dark, custom
        private String primaryColor;
        private String secondaryColor;
        private String accentColor;
        private String backgroundColor;
        private String textColor;
        private String fontFamily;
        private String headingFontFamily;
        private Integer borderRadius;
        private Map<String, Object> customVariables;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeoSettingsDTO {
        private String title;
        private String description;
        private String keywords;
        private String ogTitle;
        private String ogDescription;
        private String ogImage;
        private String twitterCard;
        private String canonicalUrl;
        private Boolean noIndex;
        private Boolean noFollow;
        private Map<String, String> customMeta;
    }
}
