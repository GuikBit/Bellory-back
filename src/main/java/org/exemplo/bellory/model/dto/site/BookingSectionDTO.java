package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.tenent.FuncionarioPublicDTO;
import org.exemplo.bellory.model.dto.tenent.ServicoPublicDTO;

import java.util.List;

/**
 * DTO para a seção de agendamento do site público.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingSectionDTO {

    private String title;
    private String subtitle;
    private Boolean enabled;

    /**
     * Serviços disponíveis para agendamento
     */
    private List<ServicoPublicDTO> servicosDisponiveis;

    /**
     * Profissionais disponíveis para agendamento
     */
    private List<FuncionarioPublicDTO> profissionaisDisponiveis;

    /**
     * Configurações do agendamento
     */
    private BookingConfigDTO config;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingConfigDTO {
        private Boolean requiresDeposit;
        private Double depositPercentage;
        private Integer minAdvanceHours;
        private Integer maxAdvanceDays;
        private Boolean allowMultipleServices;
        private Boolean requiresLogin;
    }
}
