package org.exemplo.bellory.model.dto.landingpage.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request para gerar conteúdo de seção usando IA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIGenerateSectionRequest {

    /**
     * Tipo da seção a gerar: HERO, ABOUT, SERVICES, etc.
     */
    private String sectionType;

    /**
     * Prompt/instruções para a IA.
     */
    private String prompt;

    /**
     * Estilo desejado: professional, casual, modern, classic, bold
     */
    private String style;

    /**
     * Tom: formal, informal, friendly, serious
     */
    private String tone;

    /**
     * Contexto adicional sobre o negócio.
     */
    private String businessContext;

    /**
     * Dados do negócio para usar como variáveis.
     */
    private Map<String, Object> businessData;

    /**
     * Template base a usar como referência.
     */
    private String templateId;

    /**
     * Se deve incluir imagens sugeridas.
     */
    @Builder.Default
    private Boolean suggestImages = true;

    /**
     * Se deve gerar variações.
     */
    @Builder.Default
    private Integer variations = 1;

    /**
     * Idioma do conteúdo.
     */
    @Builder.Default
    private String language = "pt-BR";
}
