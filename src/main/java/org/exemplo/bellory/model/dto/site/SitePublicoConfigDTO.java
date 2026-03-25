package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.site.request.*;

import java.time.LocalDateTime;

/**
 * DTO de resposta com todas as configurações editáveis do site público.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SitePublicoConfigDTO {

    private Long id;
    private Long organizacaoId;

    private HeaderConfigRequest header;
    private HeroSectionRequest hero;
    private AboutSectionRequest about;
    private FooterConfigRequest footer;
    private ServicesSectionRequest services;
    private ProductsSectionRequest products;
    private TeamSectionRequest team;
    private BookingSectionRequest booking;
    private GeneralSettingsRequest general;

    private Boolean active;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;
}
