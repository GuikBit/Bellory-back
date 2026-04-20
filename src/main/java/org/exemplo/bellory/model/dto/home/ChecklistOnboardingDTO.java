package org.exemplo.bellory.model.dto.home;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistOnboardingDTO {
    private boolean empresaConfigurada;
    private boolean horariosDefinidos;
    private boolean servicosCadastrados;
    private boolean colaboradoresCadastrados;
    private boolean primeiroAgendamento;
    private boolean logoEnviada;
    private boolean planoEscolhido;
    private int percentualCompleto;
}
