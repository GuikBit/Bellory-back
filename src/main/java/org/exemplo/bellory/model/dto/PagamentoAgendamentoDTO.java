package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PagamentoAgendamentoDTO {
    private BigDecimal valorPagamento;
    private String metodoPagamento;
    private Long cartaoCreditoId;
    private String observacoes;

}
