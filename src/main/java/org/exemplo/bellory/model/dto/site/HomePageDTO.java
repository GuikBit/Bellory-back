package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.tenent.FeaturesDTO;
import org.exemplo.bellory.model.dto.tenent.OrganizacaoPublicDTO;
import org.exemplo.bellory.model.dto.tenent.SeoMetadataDTO;
import org.exemplo.bellory.model.dto.tenent.SiteConfigDTO;

import java.util.List;

/**
 * DTO que agrupa todos os dados necessários para renderizar a home page do site público.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomePageDTO {

    // ==================== INFORMAÇÕES DA ORGANIZAÇÃO ====================
    private OrganizacaoPublicDTO organizacao;

    // ==================== CONFIGURAÇÕES GERAIS DO SITE ====================
    private SiteConfigDTO siteConfig;

    // ==================== SEÇÕES DA HOME ====================

    /**
     * Configuração do header
     */
    private HeaderConfigDTO header;

    /**
     * Seção hero/banner principal
     */
    private HeroSectionDTO hero;

    /**
     * Seção sobre (versão resumida para home)
     */
    private AboutSectionDTO about;

    /**
     * Seção de serviços em destaque
     */
    private ServicesSectionDTO services;

    /**
     * Seção de produtos em destaque
     */
    private ProductsSectionDTO products;

    /**
     * Seção da equipe
     */
    private TeamSectionDTO team;

    /**
     * Seção de agendamento
     */
    private BookingSectionDTO booking;

    /**
     * Configuração do footer
     */
    private FooterConfigDTO footer;

    // ==================== METADADOS ====================

    /**
     * Ordem das seções na home
     */
    private List<String> sectionsOrder;

    /**
     * Metadados SEO
     */
    private SeoMetadataDTO seo;

    /**
     * Features habilitadas
     */
    private FeaturesDTO features;

    /**
     * Scripts e estilos customizados
     */
    private CustomAssetsDTO customAssets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomAssetsDTO {
        private String customCss;
        private String customJs;
        private List<ExternalScriptDTO> externalScripts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalScriptDTO {
        private String name;
        private String script;
        private String position; // head, body-start, body-end
    }
}
