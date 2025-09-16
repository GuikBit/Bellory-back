package org.exemplo.bellory.model.entity.tenant;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Entidade que representa uma página configurável de um tenant.
 * Cada página pode ter múltiplos componentes organizados por ordem.
 */
@Entity
@Table(name = "page", indexes = {
        @Index(name = "idx_page_tenant_slug", columnList = "tenant_id, slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonBackReference("tenant-pages")
    private Tenant tenant;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // SEO e Meta dados
    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "meta_keywords", columnDefinition = "TEXT")
    private String metaKeywords;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    // Relacionamento com os componentes da página
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference("page-components")
    @OrderBy("orderIndex ASC") // Garante que os componentes vêm ordenados
    private List<PageComponent> components = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }

    /**
     * Método utilitário para adicionar um componente à página.
     * @param component O componente a ser adicionado
     */
    public void addComponent(PageComponent component) {
        if (components == null) {
            components = new ArrayList<>();
        }
        components.add(component);
        component.setPage(this);
    }

    /**
     * Método utilitário para remover um componente da página.
     * @param component O componente a ser removido
     */
    public void removeComponent(PageComponent component) {
        if (components != null) {
            components.remove(component);
            component.setPage(null);
        }
    }

    /**
     * Retorna os componentes ordenados por orderIndex.
     * @return Lista de componentes ordenada
     */
    public List<PageComponent> getOrderedComponents() {
        if (components == null) {
            return new ArrayList<>();
        }
        return components.stream()
                .sorted(Comparator.comparingInt(PageComponent::getOrderIndex))
                .toList();
    }

    /**
     * Reordena os componentes com base nos novos índices.
     * @param componentIds Lista de IDs dos componentes na nova ordem
     */
    public void reorderComponents(List<Long> componentIds) {
        if (components == null || componentIds == null) {
            return;
        }

        for (int i = 0; i < componentIds.size(); i++) {
            Long componentId = componentIds.get(i);
            int finalI = i;
            components.stream()
                    .filter(c -> c.getId().equals(componentId))
                    .findFirst()
                    .ifPresent(c -> c.setOrderIndex(finalI));
        }
    }
}
