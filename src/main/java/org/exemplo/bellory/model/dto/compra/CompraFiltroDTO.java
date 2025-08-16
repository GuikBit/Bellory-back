package org.exemplo.bellory.model.dto.compra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompraFiltroDTO {
    private Long clienteId;
    private String status;
    private LocalDate dataInicio;
    private LocalDate dataFim;
}
