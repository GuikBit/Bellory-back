package org.exemplo.bellory.model.dto.landingpage;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO de uma seção da Landing Page.
 * Contém a estrutura flexível de elementos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LandingPageSectionDTO {

    /**
     * ID no banco de dados.
     */
    private Long id;

    /**
     * ID único da seção (UUID).
     */
    private String sectionId;

    /**
     * Tipo da seção: HEADER, HERO, ABOUT, etc.
     */
    private String tipo;

    /**
     * Nome da seção para exibição no editor.
     */
    private String nome;

    /**
     * Ordem de exibição.
     */
    private Integer ordem;

    /**
     * Se está visível.
     */
    private Boolean visivel;

    /**
     * Template/variante: hero-centered, hero-split, etc.
     */
    private String template;

    /**
     * Conteúdo da seção (estrutura de elementos).
     */
    private SectionContentDTO content;

    /**
     * Estilos da seção (wrapper).
     */
    private SectionStylesDTO styles;

    /**
     * Configurações específicas da seção.
     */
    private Map<String, Object> settings;

    /**
     * Animações.
     */
    private AnimationDTO animations;

    /**
     * Regras de visibilidade por device.
     */
    private VisibilityRulesDTO visibilityRules;

    /**
     * Fonte de dados dinâmicos.
     */
    private DataSourceDTO dataSource;

    /**
     * Se está bloqueada para edição.
     */
    private Boolean locked;

    // ==================== SUB DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionContentDTO {
        /**
         * Layout da seção: centered, left, right, split, full-width
         */
        private String layout;

        /**
         * Configuração de background.
         */
        private BackgroundDTO background;

        /**
         * Lista de elementos da seção.
         */
        private List<ElementDTO> elements;

        /**
         * Configuração do container.
         */
        private ContainerDTO container;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackgroundDTO {
        private String type; // color, image, video, gradient
        private String color;
        private String imageUrl;
        private String videoUrl;
        private String gradient;
        private Double overlay;
        private String overlayColor;
        private String position; // center, top, bottom, etc.
        private String size; // cover, contain, auto
        private String repeat; // no-repeat, repeat, repeat-x, repeat-y
        private String attachment; // scroll, fixed
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContainerDTO {
        private String maxWidth; // full, xl, lg, md, sm ou valor em px
        private Boolean centered;
        private String padding;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionStylesDTO {
        private ResponsiveStyleDTO desktop;
        private ResponsiveStyleDTO tablet;
        private ResponsiveStyleDTO mobile;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponsiveStyleDTO {
        private String padding;
        private String paddingTop;
        private String paddingBottom;
        private String paddingLeft;
        private String paddingRight;
        private String margin;
        private String marginTop;
        private String marginBottom;
        private String minHeight;
        private String maxHeight;
        private String backgroundColor;
        private String borderRadius;
        private String boxShadow;
        private Map<String, String> customStyles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnimationDTO {
        private String type; // fadeIn, slideUp, slideDown, zoomIn, etc.
        private Integer duration; // ms
        private Integer delay; // ms
        private String easing; // ease, ease-in, ease-out, ease-in-out
        private Boolean repeat;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisibilityRulesDTO {
        private Boolean showOnDesktop;
        private Boolean showOnTablet;
        private Boolean showOnMobile;
        private String startDate; // Para seções temporárias
        private String endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceDTO {
        /**
         * Tipo de dados: services, products, team, testimonials, custom
         */
        private String source;

        /**
         * Filtros a aplicar.
         */
        private Map<String, Object> filter;

        /**
         * Limite de itens.
         */
        private Integer limit;

        /**
         * Ordenação.
         */
        private String orderBy;

        /**
         * Direção: asc, desc
         */
        private String orderDirection;
    }
}
