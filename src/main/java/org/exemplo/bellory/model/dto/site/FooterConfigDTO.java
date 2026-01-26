package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.tenent.EnderecoPublicDTO;
import org.exemplo.bellory.model.dto.tenent.HorarioFuncionamentoDTO;

import java.util.List;

/**
 * DTO para configuração do footer do site público.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FooterConfigDTO {

    private String logoUrl;
    private String description;
    private String copyrightText;

    private List<LinkSectionDTO> linkSections;

    private ContactInfoDTO contactInfo;
    private HeaderConfigDTO.SocialLinksDTO socialLinks;

    private Boolean showMap;
    private Boolean showHours;
    private Boolean showSocial;
    private Boolean showNewsletter;

    private List<HorarioFuncionamentoDTO> horariosFuncionamento;
    private EnderecoPublicDTO endereco;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkSectionDTO {
        private String title;
        private List<LinkDTO> links;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkDTO {
        private String label;
        private String href;
        private Boolean external;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfoDTO {
        private String phone;
        private String whatsapp;
        private String email;
        private String address;
    }
}
