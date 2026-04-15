package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.tenent.FuncionarioPublicDTO;

import java.util.List;

/**
 * DTO para a seção de equipe do site público.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamSectionDTO {

    private String title;
    private String subtitle;
    private Boolean showSection;
    private String layout;
    private String cardStyle;
    private String photoShape;
    private Integer photoHeight;
    private Boolean showBio;
    private Boolean showServices;
    private Boolean showSchedule;
    private Boolean carouselAutoPlay;
    private Integer carouselSpeed;
    private String backgroundColor;
    private String backgroundPattern;
    private Double patternOpacity;

    private List<FuncionarioPublicDTO> membros;

    /**
     * Total de membros da equipe
     */
    private Integer totalMembros;
}
