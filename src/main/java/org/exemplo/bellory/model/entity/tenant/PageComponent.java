package org.exemplo.bellory.model.entity.tenant;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade que representa um componente/bloco configurável de uma página.
 * Cada componente tem um tipo (ex: "HERO", "SERVICES", "CONTACT") e propriedades em JSON.
 */
@Entity
@Table(name = "page_component", indexes = {
        @Index(name = "idx_component_page_order", columnList = "page_id, order_index")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    @JsonBackReference("page-components")
    private Page page;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // Ex: "HERO", "SERVICES_GRID", "CONTACT_FORM", "IMAGE_GALLERY"

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // Propriedades do componente armazenadas como JSON
    @Lob
    @Column(name = "props_json", columnDefinition = "TEXT")
    private String propsJson;

    // Configurações de estilo específicas do componente
    @Column(name = "style_config", columnDefinition = "TEXT")
    private String styleConfig;

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

    /**
     * Enum com os tipos de componentes disponíveis.
     * Pode ser expandido conforme necessário.
     */
    public enum ComponentType {
        HERO("HERO", "Seção principal/banner"),
        SERVICES_GRID("SERVICES_GRID", "Grade de serviços"),
        ABOUT("ABOUT", "Seção sobre"),
        TEAM("TEAM", "Equipe"),
        CONTACT_FORM("CONTACT_FORM", "Formulário de contato"),
        IMAGE_GALLERY("IMAGE_GALLERY", "Galeria de imagens"),
        TESTIMONIALS("TESTIMONIALS", "Depoimentos"),
        FEATURES("FEATURES", "Características/recursos"),
        PRICING("PRICING", "Tabela de preços"),
        FAQ("FAQ", "Perguntas frequentes"),
        MAP("MAP", "Mapa/localização"),
        SOCIAL_MEDIA("SOCIAL_MEDIA", "Redes sociais"),
        NEWSLETTER("NEWSLETTER", "Newsletter/inscrição"),
        CUSTOM("CUSTOM", "Componente customizado");

        private final String value;
        private final String description;

        ComponentType(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static ComponentType fromValue(String value) {
            for (ComponentType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid component type: " + value);
        }
    }
}
