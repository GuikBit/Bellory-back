package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AgendamentoEstatisticasDTO {
    private long totalAgendamentos;
    private long agendamentosPendentes;
    private long agendamentosConfirmados;
    private long agendamentosConcluidos;
    private long agendamentosCancelados;
    private long agendamentosHoje;
}
