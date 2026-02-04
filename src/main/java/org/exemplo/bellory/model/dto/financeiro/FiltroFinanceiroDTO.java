package org.exemplo.bellory.model.dto.financeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FiltroFinanceiroDTO {
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String status;
    private String tipo;
    private Long categoriaFinanceiraId;
    private Long centroCustoId;
    private Long contaBancariaId;
    private Long clienteId;
    private String fornecedor;
    private Boolean recorrente;
}
