package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.tenent.FuncionarioPublicDTO;
import org.exemplo.bellory.model.dto.tenent.ServicoPublicDTO;

import java.time.LocalDate;
import java.time.LocalTime;
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
    private String backgroundColor;
    private String backgroundPattern;
    private Double patternOpacity;

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

    /**
     * Dias bloqueados (feriados e bloqueios da organização)
     */
    private List<BloqueioDTO> diasBloqueados;

    /**
     * Horários de funcionamento por dia da semana
     */
    private List<HorarioFuncionamentoDTO> horariosFuncionamento;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingConfigDTO {
        private Boolean requiresDeposit;
        private Double depositPercentage;
        private Integer minAdvanceHours;
        private Integer minAdvanceDays;
        private Integer maxAdvanceDays;
        private Boolean allowMultipleServices;
        private Boolean requiresLogin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BloqueioDTO {
        private String titulo;
        private LocalDate dataInicio;
        private LocalDate dataFim;
        private String tipo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HorarioFuncionamentoDTO {
        private String diaSemana;
        private Boolean ativo;
        private List<PeriodoDTO> periodos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodoDTO {
        private LocalTime horaInicio;
        private LocalTime horaFim;
    }
}
