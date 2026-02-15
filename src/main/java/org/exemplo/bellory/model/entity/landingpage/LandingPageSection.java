package org.exemplo.bellory.model.entity.landingpage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa uma seção dentro de uma Landing Page.
 * Cada seção contém elementos configuráveis via JSON flexível.
 *
 * Tipos de seção suportados:
 * - HEADER: Menu de navegação
 * - HERO: Banner principal
 * - ABOUT: Sobre a empresa
 * - SERVICES: Lista de serviços
 * - PRODUCTS: Lista de produtos
 * - TEAM: Equipe
 * - TESTIMONIALS: Depoimentos
 * - PRICING: Planos/preços
 * - GALLERY: Galeria de imagens
 * - CONTACT: Formulário de contato
 * - BOOKING: Agendamento
 * - FAQ: Perguntas frequentes
 * - CTA: Call to action
 * - FOOTER: Rodapé
 * - CUSTOM: Seção customizada
 */
@Entity
@Table(name = "landing_page_section", schema = "app", indexes = {
        @Index(name = "idx_section_landing_page", columnList = "landing_page_id"),
        @Index(name = "idx_section_order", columnList = "landing_page_id, ordem")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandingPageSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landing_page_id", nullable = false)
    @JsonIgnore
    private LandingPage landingPage;

    /**
     * Identificador único da seção (UUID ou slug).
     * Usado para referência no frontend.
     */
    @Column(name = "section_id", nullable = false, length = 100)
    private String sectionId;

    /**
     * Tipo da seção: HEADER, HERO, ABOUT, SERVICES, etc.
     */
    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    /**
     * Nome da seção para exibição no editor.
     */
    @Column(name = "nome", length = 200)
    private String nome;

    /**
     * Ordem de exibição da seção na página.
     */
    @Column(name = "ordem", nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    /**
     * Se a seção está visível.
     */
    @Column(name = "visivel")
    @Builder.Default
    private Boolean visivel = true;

    /**
     * Template/variante da seção.
     * Ex: "hero-centered", "hero-split", "hero-video"
     */
    @Column(name = "template", length = 100)
    private String template;

    /**
     * Conteúdo da seção em formato JSON.
     * Estrutura flexível que contém todos os elementos.
     *
     * Exemplo para HERO:
     * {
     *   "layout": "centered",
     *   "background": {
     *     "type": "image",
     *     "url": "https://...",
     *     "overlay": 0.5,
     *     "overlayColor": "#000000"
     *   },
     *   "elements": [
     *     {
     *       "id": "title-1",
     *       "type": "heading",
     *       "tag": "h1",
     *       "content": "Bem-vindo à {empresa}",
     *       "styles": {
     *         "desktop": {"fontSize": "48px", "fontWeight": "bold"},
     *         "tablet": {"fontSize": "36px"},
     *         "mobile": {"fontSize": "28px"}
     *       }
     *     },
     *     {
     *       "id": "btn-1",
     *       "type": "button",
     *       "content": "Agendar Agora",
     *       "href": "#booking",
     *       "variant": "primary",
     *       "styles": {...}
     *     }
     *   ],
     *   "spacing": {
     *     "desktop": {"paddingY": "120px"},
     *     "tablet": {"paddingY": "80px"},
     *     "mobile": {"paddingY": "60px"}
     *   }
     * }
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Estilos da seção (wrapper).
     * JSON com estilos responsivos.
     */
    @Column(name = "styles", columnDefinition = "TEXT")
    private String styles;

    /**
     * Configurações adicionais da seção.
     * JSON com configurações específicas do tipo.
     */
    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings;

    /**
     * Animações da seção.
     * JSON: {"type": "fadeIn", "duration": 500, "delay": 0}
     */
    @Column(name = "animations", columnDefinition = "TEXT")
    private String animations;

    /**
     * Condições de exibição.
     * JSON: {"showOnMobile": true, "showOnTablet": true, "showOnDesktop": true}
     */
    @Column(name = "visibility_rules", columnDefinition = "TEXT")
    private String visibilityRules;

    /**
     * Dados dinâmicos que a seção deve buscar.
     * JSON: {"source": "services", "filter": {"isHome": true}, "limit": 6}
     */
    @Column(name = "data_source", columnDefinition = "TEXT")
    private String dataSource;

    /**
     * Se a seção está bloqueada para edição (seções padrão).
     */
    @Column(name = "locked")
    @Builder.Default
    private Boolean locked = false;

    @Column(name = "ativo")
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
        if (sectionId == null) {
            sectionId = java.util.UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }
}
