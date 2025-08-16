package org.exemplo.bellory.model.dto.clienteDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoFiltroDTO {
    private Long clienteId;
    private String tipo; // "AGENDAMENTO", "COMPRA", "TODOS"
    private LocalDate dataInicio;
    private LocalDate dataFim;
}
