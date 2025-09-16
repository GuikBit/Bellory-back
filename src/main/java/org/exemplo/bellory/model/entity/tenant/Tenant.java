package org.exemplo.bellory.model.entity.tenant;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um tenant (cliente) no sistema multi-tenant.
 * Cada tenant tem suas próprias páginas e configurações.
 */
@Entity
@Table(name = "tenant", indexes = {
        @Index(name = "idx_tenant_subdomain", columnList = "subdomain", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "subdomain", nullable = false, unique = true, length = 100)
    private String subdomain;

    @Column(name = "theme", length = 50)
    private String theme = "default";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Configurações de tema/personalização armazenadas como JSON
    @Column(name = "theme_config", columnDefinition = "TEXT")
    private String themeConfig;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    // Relacionamento com as páginas do tenant
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("tenant-pages")
    private List<Page> pages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }

    /**
     * Método utilitário para adicionar uma página ao tenant.
     * @param page A página a ser adicionada
     */
    public void addPage(Page page) {
        if (pages == null) {
            pages = new ArrayList<>();
        }
        pages.add(page);
        page.setTenant(this);
    }

    /**
     * Método utilitário para remover uma página do tenant.
     * @param page A página a ser removida
     */
    public void removePage(Page page) {
        if (pages != null) {
            pages.remove(page);
            page.setTenant(null);
        }
    }
}
