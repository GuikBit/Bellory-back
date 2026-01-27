package org.exemplo.bellory.model.dto.landingpage;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para versão de uma Landing Page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LandingPageVersionDTO {
    private Long id;
    private Integer versao;
    private String descricao;
    private String tipo;
    private Long criadoPor;
    private String criadoPorNome;
    private LocalDateTime dtCriacao;

    /**
     * Snapshot completo (só incluído quando solicitado).
     */
    private LandingPageDTO snapshot;
}
