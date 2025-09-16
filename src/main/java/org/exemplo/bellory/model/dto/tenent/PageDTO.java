package org.exemplo.bellory.model.dto.tenant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para transferência de dados de páginas.
 * Usado para serialização/deserialização nas APIs REST.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageDTO {

    private Long id;

    private String slug;

    private String title;

    private String description;

    private boolean active;

    @JsonProperty("meta_title")
    private String metaTitle;

    @JsonProperty("meta_description")
    private String metaDescription;

    @JsonProperty("meta_keywords")
    private String metaKeywords;

    @JsonProperty("created_at")
    private LocalDateTime dtCriacao;

    @JsonProperty("updated_at")
    private LocalDateTime dtAtualizacao;

    // Informações do tenant (opcional, dependendo do contexto)
    private TenantSummaryDTO tenant;

    // Lista de componentes da página
    private List<PageComponentDTO> components;

    // Estatísticas opcionais
    @JsonProperty("component_count")
    private Integer componentCount;

    /**
     * DTO resumido para informações básicas do tenant.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TenantSummaryDTO {
        private Long id;
        private String name;
        private String subdomain;
        private String theme;
    }
}
