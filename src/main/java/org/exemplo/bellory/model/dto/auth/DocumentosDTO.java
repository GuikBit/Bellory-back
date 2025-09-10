package org.exemplo.bellory.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentosDTO {
    private String rg;
    private String rgOrgEmissor;
    private String tituloEleitor;
    private String certMilitar;
    private String cnh;
    private String categHabilitacao;
    private String ctps;
    private String ctpsSerie;
    private String pisPasep;
}
