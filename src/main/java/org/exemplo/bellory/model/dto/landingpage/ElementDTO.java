package org.exemplo.bellory.model.dto.landingpage;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO que representa um elemento dentro de uma seção.
 * Estrutura flexível para qualquer tipo de elemento.
 *
 * A estrutura é projetada para ser:
 * 1. Flexível - suporta qualquer tipo de elemento
 * 2. Responsiva - estilos diferentes por breakpoint
 * 3. Aninhável - elementos podem conter outros elementos
 * 4. IA-friendly - estrutura clara para geração por IA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElementDTO {

    /**
     * ID único do elemento (UUID).
     */
    private String id;

    /**
     * Tipo do elemento: heading, paragraph, button, image, card, etc.
     */
    private String type;

    /**
     * Tag HTML (para elementos de texto): h1, h2, h3, p, span, etc.
     */
    private String tag;

    /**
     * Conteúdo textual do elemento.
     * Suporta variáveis: {empresa}, {telefone}, {endereco}
     */
    private String content;

    /**
     * Conteúdo HTML (para rich text).
     */
    private String htmlContent;

    /**
     * URL (para links, imagens, vídeos).
     */
    private String url;

    /**
     * URL alternativa (para imagens responsivas).
     */
    private String urlMobile;

    /**
     * Texto alternativo (para imagens).
     */
    private String alt;

    /**
     * Ação do elemento (para botões e links).
     */
    private ActionDTO action;

    /**
     * Ícone do elemento.
     */
    private IconDTO icon;

    /**
     * Elementos filhos (para containers, grids, cards).
     */
    private List<ElementDTO> children;

    /**
     * Estilos responsivos do elemento.
     */
    private ElementStylesDTO styles;

    /**
     * Variante/estilo pré-definido: primary, secondary, outline, ghost
     */
    private String variant;

    /**
     * Tamanho: xs, sm, md, lg, xl
     */
    private String size;

    /**
     * Propriedades específicas do tipo de elemento.
     */
    private Map<String, Object> props;

    /**
     * Animação do elemento.
     */
    private LandingPageSectionDTO.AnimationDTO animation;

    /**
     * Se o elemento está visível.
     */
    private Boolean visible;

    /**
     * Regras de visibilidade por device.
     */
    private LandingPageSectionDTO.VisibilityRulesDTO visibilityRules;

    /**
     * Referência a dados dinâmicos.
     * Ex: {field: "servico.nome"} para exibir nome do serviço
     */
    private String dataBinding;

    /**
     * Condição para renderização.
     * Ex: "servico.preco > 0"
     */
    private String condition;

    /**
     * Ordem de exibição (para ordenação manual).
     */
    private Integer order;

    // ==================== SUB DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionDTO {
        /**
         * Tipo: link, scroll, modal, submit, whatsapp, phone, email, external
         */
        private String type;

        /**
         * URL ou identificador do destino.
         */
        private String href;

        /**
         * Target: _self, _blank
         */
        private String target;

        /**
         * ID do modal (para type: modal).
         */
        private String modalId;

        /**
         * Mensagem (para WhatsApp).
         */
        private String message;

        /**
         * Evento de tracking.
         */
        private String trackingEvent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IconDTO {
        /**
         * Nome do ícone (ex: "arrow-right", "phone", "star").
         */
        private String name;

        /**
         * Biblioteca: lucide, heroicons, fontawesome, custom
         */
        private String library;

        /**
         * Posição: left, right, top, bottom
         */
        private String position;

        /**
         * Tamanho em pixels ou classe.
         */
        private String size;

        /**
         * Cor do ícone.
         */
        private String color;

        /**
         * URL do ícone customizado (SVG).
         */
        private String customUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ElementStylesDTO {
        private ElementResponsiveStyleDTO desktop;
        private ElementResponsiveStyleDTO tablet;
        private ElementResponsiveStyleDTO mobile;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ElementResponsiveStyleDTO {
        // Layout
        private String display;
        private String position;
        private String top;
        private String right;
        private String bottom;
        private String left;
        private String zIndex;

        // Flexbox
        private String flexDirection;
        private String justifyContent;
        private String alignItems;
        private String gap;
        private String flex;

        // Grid
        private String gridTemplateColumns;
        private String gridTemplateRows;
        private String gridColumn;
        private String gridRow;

        // Sizing
        private String width;
        private String height;
        private String minWidth;
        private String maxWidth;
        private String minHeight;
        private String maxHeight;

        // Spacing
        private String padding;
        private String paddingTop;
        private String paddingRight;
        private String paddingBottom;
        private String paddingLeft;
        private String margin;
        private String marginTop;
        private String marginRight;
        private String marginBottom;
        private String marginLeft;

        // Typography
        private String fontSize;
        private String fontWeight;
        private String fontFamily;
        private String lineHeight;
        private String letterSpacing;
        private String textAlign;
        private String textTransform;
        private String textDecoration;
        private String color;

        // Background
        private String backgroundColor;
        private String backgroundImage;
        private String backgroundSize;
        private String backgroundPosition;

        // Border
        private String border;
        private String borderRadius;
        private String borderColor;
        private String borderWidth;
        private String borderStyle;

        // Effects
        private String boxShadow;
        private String opacity;
        private String transform;
        private String transition;
        private String cursor;
        private String overflow;

        // Custom CSS
        private Map<String, String> custom;
    }
}
