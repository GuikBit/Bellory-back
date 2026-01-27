package org.exemplo.bellory.model.entity.landingpage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade principal que representa uma Landing Page completa.
 * Cada organização pode ter múltiplas landing pages (home, promoções, eventos, etc.)
 */
@Entity
@Table(name = "landing_page", schema = "app", indexes = {
        @Index(name = "idx_landing_page_org", columnList = "organizacao_id"),
        @Index(name = "idx_landing_page_slug", columnList = "organizacao_id, slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandingPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    /**
     * Identificador único da página dentro da organização.
     * Ex: "home", "promocao-verao", "natal-2024"
     */
    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    /**
     * Nome amigável da página para exibição no editor.
     */
    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    /**
     * Descrição da página (para uso interno).
     */
    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    /**
     * Tipo da página: HOME, PROMOCAO, EVENTO, CAMPANHA, CUSTOM
     */
    @Column(name = "tipo", length = 50)
    @Builder.Default
    private String tipo = "HOME";

    /**
     * Se é a página principal/home da organização.
     */
    @Column(name = "is_home")
    @Builder.Default
    private Boolean isHome = false;

    /**
     * Status: DRAFT, PUBLISHED, ARCHIVED
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "DRAFT";

    /**
     * Seções da landing page.
     */
    @OneToMany(mappedBy = "landingPage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC")
    @Builder.Default
    private List<LandingPageSection> sections = new ArrayList<>();

    /**
     * Configurações globais da página (tema, fontes, cores).
     * JSON: {"theme": "dark", "primaryColor": "#ff6b00", "fontFamily": "Inter"}
     */
    @Column(name = "global_settings", columnDefinition = "TEXT")
    private String globalSettings;

    /**
     * Configurações de SEO da página.
     * JSON: {"title": "...", "description": "...", "keywords": "...", "ogImage": "..."}
     */
    @Column(name = "seo_settings", columnDefinition = "TEXT")
    private String seoSettings;

    /**
     * CSS customizado adicional.
     */
    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss;

    /**
     * JavaScript customizado adicional.
     */
    @Column(name = "custom_js", columnDefinition = "TEXT")
    private String customJs;

    /**
     * Favicon customizado.
     */
    @Column(name = "favicon_url", length = 500)
    private String faviconUrl;

    /**
     * Versão atual da página (para controle de cache e histórico).
     */
    @Column(name = "versao")
    @Builder.Default
    private Integer versao = 1;

    /**
     * Data de publicação.
     */
    @Column(name = "dt_publicacao")
    private LocalDateTime dtPublicacao;

    /**
     * Usuário que publicou.
     */
    @Column(name = "publicado_por")
    private Long publicadoPor;

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
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }

    // Helper methods
    public void addSection(LandingPageSection section) {
        sections.add(section);
        section.setLandingPage(this);
    }

    public void removeSection(LandingPageSection section) {
        sections.remove(section);
        section.setLandingPage(null);
    }
}
