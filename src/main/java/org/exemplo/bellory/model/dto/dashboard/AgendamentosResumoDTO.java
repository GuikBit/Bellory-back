package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendamentosResumoDTO {
    private Long total;
    private Long hoje;
    private Long estaSemana;
    private Long esteMes;
    private Map<String, Long> porStatus;
    private Long pendentes;
    private Long agendados;
    private Long concluidos;
    private Long cancelados;
    private Long emEspera;
    private Double taxaOcupacao;
    private Double taxaCancelamento;
    private Long proximosAgendamentos;
    private List<AgendamentoRecenteDTO> recentes;
}
