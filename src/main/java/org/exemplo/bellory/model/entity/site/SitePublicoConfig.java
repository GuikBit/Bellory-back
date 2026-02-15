package org.exemplo.bellory.model.entity.site;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;

/**
 * Entidade que armazena as configurações do site público da organização.
 * Controla header, hero, about, footer e outras seções customizáveis.
 */
@Entity
@Table(name = "site_publico_config", schema = "app", indexes = {
        @Index(name = "idx_site_config_org", columnList = "organizacao_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SitePublicoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false, unique = true)
    @JsonIgnore
    private Organizacao organizacao;

    // ==================== HEADER CONFIG ====================

    @Column(name = "header_logo_url", length = 500)
    private String headerLogoUrl;

    @Column(name = "header_logo_alt", length = 100)
    private String headerLogoAlt;

    /**
     * JSON com os itens do menu do header.
     * Estrutura: [{"label": "Início", "href": "/", "order": 1}, ...]
     */
    @Column(name = "header_menu_items", columnDefinition = "TEXT")
    private String headerMenuItems;

    /**
     * JSON com os botões de ação do header.
     * Estrutura: [{"label": "Agendar", "href": "/agendar", "type": "primary"}, ...]
     */
    @Column(name = "header_action_buttons", columnDefinition = "TEXT")
    private String headerActionButtons;

    @Column(name = "header_show_phone")
    @Builder.Default
    private Boolean headerShowPhone = true;

    @Column(name = "header_show_social")
    @Builder.Default
    private Boolean headerShowSocial = false;

    @Column(name = "header_sticky")
    @Builder.Default
    private Boolean headerSticky = true;

    // ==================== HERO CONFIG ====================

    /**
     * Tipo do hero: TEMPLATE ou CUSTOM_HTML
     */
    @Column(name = "hero_type", length = 20)
    @Builder.Default
    private String heroType = "TEMPLATE";

    @Column(name = "hero_title", length = 255)
    private String heroTitle;

    @Column(name = "hero_subtitle", columnDefinition = "TEXT")
    private String heroSubtitle;

    @Column(name = "hero_background_url", length = 500)
    private String heroBackgroundUrl;

    @Column(name = "hero_background_overlay")
    @Builder.Default
    private Double heroBackgroundOverlay = 0.5;

    /**
     * HTML customizado para o hero (quando heroType = CUSTOM_HTML)
     */
    @Column(name = "hero_custom_html", columnDefinition = "TEXT")
    private String heroCustomHtml;

    /**
     * JSON com botões do hero.
     * Estrutura: [{"label": "Agendar Agora", "href": "/agendar", "type": "primary"}]
     */
    @Column(name = "hero_buttons", columnDefinition = "TEXT")
    private String heroButtons;

    @Column(name = "hero_show_booking_form")
    @Builder.Default
    private Boolean heroShowBookingForm = false;

    // ==================== ABOUT SECTION ====================

    @Column(name = "about_title", length = 255)
    private String aboutTitle;

    @Column(name = "about_subtitle", length = 255)
    private String aboutSubtitle;

    @Column(name = "about_description", columnDefinition = "TEXT")
    private String aboutDescription;

    /**
     * Descrição completa para a página "Sobre" dedicada
     */
    @Column(name = "about_full_description", columnDefinition = "TEXT")
    private String aboutFullDescription;

    @Column(name = "about_image_url", length = 500)
    private String aboutImageUrl;

    /**
     * JSON com múltiplas imagens para galeria.
     * Estrutura: ["url1", "url2", ...]
     */
    @Column(name = "about_gallery_images", columnDefinition = "TEXT")
    private String aboutGalleryImages;

    @Column(name = "about_video_url", length = 500)
    private String aboutVideoUrl;

    /**
     * JSON com highlights/diferenciais.
     * Estrutura: [{"icon": "star", "title": "10+ Anos", "description": "De experiência"}]
     */
    @Column(name = "about_highlights", columnDefinition = "TEXT")
    private String aboutHighlights;

    @Column(name = "about_mission", columnDefinition = "TEXT")
    private String aboutMission;

    @Column(name = "about_vision", columnDefinition = "TEXT")
    private String aboutVision;

    @Column(name = "about_values", columnDefinition = "TEXT")
    private String aboutValues;

    // ==================== SERVICES SECTION ====================

    @Column(name = "services_section_title", length = 255)
    @Builder.Default
    private String servicesSectionTitle = "Nossos Serviços";

    @Column(name = "services_section_subtitle", length = 255)
    private String servicesSectionSubtitle;

    @Column(name = "services_show_prices")
    @Builder.Default
    private Boolean servicesShowPrices = true;

    @Column(name = "services_show_duration")
    @Builder.Default
    private Boolean servicesShowDuration = true;

    @Column(name = "services_featured_limit")
    @Builder.Default
    private Integer servicesFeaturedLimit = 6;

    // ==================== PRODUCTS SECTION ====================

    @Column(name = "products_section_title", length = 255)
    @Builder.Default
    private String productsSectionTitle = "Produtos em Destaque";

    @Column(name = "products_section_subtitle", length = 255)
    private String productsSectionSubtitle;

    @Column(name = "products_show_prices")
    @Builder.Default
    private Boolean productsShowPrices = true;

    @Column(name = "products_featured_limit")
    @Builder.Default
    private Integer productsFeaturedLimit = 8;

    // ==================== TEAM SECTION ====================

    @Column(name = "team_section_title", length = 255)
    @Builder.Default
    private String teamSectionTitle = "Nossa Equipe";

    @Column(name = "team_section_subtitle", length = 255)
    private String teamSectionSubtitle;

    @Column(name = "team_show_section")
    @Builder.Default
    private Boolean teamShowSection = true;

    // ==================== BOOKING SECTION ====================

    @Column(name = "booking_section_title", length = 255)
    @Builder.Default
    private String bookingSectionTitle = "Agende seu Horário";

    @Column(name = "booking_section_subtitle", length = 255)
    private String bookingSectionSubtitle;

    @Column(name = "booking_enabled")
    @Builder.Default
    private Boolean bookingEnabled = true;

    // ==================== FOOTER CONFIG ====================

    @Column(name = "footer_description", columnDefinition = "TEXT")
    private String footerDescription;

    @Column(name = "footer_logo_url", length = 500)
    private String footerLogoUrl;

    /**
     * JSON com links do footer organizados em seções.
     * Estrutura: [{"title": "Links", "links": [{"label": "Início", "href": "/"}]}]
     */
    @Column(name = "footer_link_sections", columnDefinition = "TEXT")
    private String footerLinkSections;

    @Column(name = "footer_copyright_text", length = 255)
    private String footerCopyrightText;

    @Column(name = "footer_show_map")
    @Builder.Default
    private Boolean footerShowMap = true;

    @Column(name = "footer_show_hours")
    @Builder.Default
    private Boolean footerShowHours = true;

    @Column(name = "footer_show_social")
    @Builder.Default
    private Boolean footerShowSocial = true;

    @Column(name = "footer_show_newsletter")
    @Builder.Default
    private Boolean footerShowNewsletter = false;

    // ==================== GENERAL SETTINGS ====================

    /**
     * Seções habilitadas na home (em ordem).
     * Valores: HERO, ABOUT, SERVICES, PRODUCTS, TEAM, BOOKING, TESTIMONIALS
     */
    @Column(name = "home_sections_order", columnDefinition = "TEXT")
    @Builder.Default
    private String homeSectionsOrder = "[\"HERO\",\"ABOUT\",\"SERVICES\",\"PRODUCTS\",\"TEAM\",\"BOOKING\"]";

    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss;

    @Column(name = "custom_js", columnDefinition = "TEXT")
    private String customJs;

    /**
     * JSON com scripts externos (analytics, etc).
     * Estrutura: [{"name": "Google Analytics", "script": "<script>...</script>", "position": "head"}]
     */
    @Column(name = "external_scripts", columnDefinition = "TEXT")
    private String externalScripts;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }
}
