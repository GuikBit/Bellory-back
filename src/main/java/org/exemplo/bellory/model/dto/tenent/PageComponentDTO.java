package org.exemplo.bellory.model.dto.tenant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO para transferência de dados de componentes de página.
 * Usado para serialização/deserialização nas APIs REST.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageComponentDTO {

    private Long id;

    private String type;

    @JsonProperty("order_index")
    private Integer orderIndex;

    private boolean active;

    // Propriedades do componente como JSON raw
    @JsonRawValue
    @JsonProperty("props")
    private String propsJson;

    // Configurações de estilo como JSON raw
    @JsonRawValue
    @JsonProperty("style")
    private String styleConfig;

    @JsonProperty("created_at")
    private LocalDateTime dtCriacao;

    @JsonProperty("updated_at")
    private LocalDateTime dtAtualizacao;

    // Informações do tipo de componente
    @JsonProperty("type_info")
    private ComponentTypeInfo typeInfo;

    /**
     * DTO para informações sobre o tipo de componente.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComponentTypeInfo {
        private String value;
        private String description;
    }
}
