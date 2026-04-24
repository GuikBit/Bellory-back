package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.tenent.OrganizacaoPublicDTO;

/**
 * Payload minimalista retornado para organizações no plano BÁSICO.
 *
 * Contém apenas:
 *  - Dados da organização com logo e banner
 *  - Seção de booking (agendamento)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomePageBasicaDTO {

    private OrganizacaoPublicDTO organizacao;

    private BookingSectionDTO booking;
}
