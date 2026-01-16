package org.exemplo.bellory.model.dto.config;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigAgendamentoDTO {
    private Integer toleranciaAgendamento;
    private Integer minDiasAgendamento;
    private Integer maxDiasAgendamento;
    private Boolean cancelamentoCliente;
    private Integer tempoCancelamentoCliente;
    private Boolean aprovarAgendamento;
    private Boolean aprovarAgendamentoAgente;
    private Boolean ocultarFimSemana;
    private Boolean ocultarDomingo;
    private Boolean cobrarSinal;
    private Integer porcentSinal;
    private Boolean cobrarSinalAgente;
    private Integer porcentSinalAgente;
}
