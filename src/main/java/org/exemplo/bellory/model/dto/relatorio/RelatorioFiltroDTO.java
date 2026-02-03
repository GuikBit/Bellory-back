package org.exemplo.bellory.model.dto.relatorio;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioFiltroDTO {
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Long funcionarioId;
    private Long clienteId;
    private Long servicoId;
    private List<String> status;
    private String formaPagamento;
    private String tipoNotificacao; // CONFIRMACAO, LEMBRETE
    private String agrupamento; // DIARIO, SEMANAL, MENSAL
    private Integer limite; // Limite de registros para rankings
}
