package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFiltroDTO {
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Long funcionarioId;
    private List<String> statusAgendamento;
    private List<String> statusPagamento;
    private List<Long> servicoIds;
    private Boolean clientesAtivos;
    private String tipoRelatorio; // "GERAL", "FINANCEIRO", "OPERACIONAL"
}
