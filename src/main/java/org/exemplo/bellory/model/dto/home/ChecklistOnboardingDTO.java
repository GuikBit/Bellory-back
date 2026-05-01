package org.exemplo.bellory.model.dto.home;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChecklistOnboardingDTO {
    private boolean empresaConfigurada;
    private boolean horariosDefinidos;
    private boolean servicosCadastrados;
    private boolean colaboradoresCadastrados;
    private boolean primeiroAgendamento;
    private boolean logoEnviada;
    private boolean planoEscolhido;
    private int percentualCompleto;

    private boolean setupCompleto;
    private List<EtapaOnboardingDTO> etapas;
}
