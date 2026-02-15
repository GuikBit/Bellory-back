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
class ServicoResumoDTO {
    private Long id;
    private String nome;
    private BigDecimal preco;
    private Integer tempoEstimadoMinutos;
    private BigDecimal desconto;
}
