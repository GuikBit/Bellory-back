package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TendenciaDTO {
    private String metrica;
    private BigDecimal valorAtual;
    private BigDecimal valorAnterior;
    private Double percentualMudanca;
    private String tendencia; // "ALTA", "BAIXA", "ESTAVEL"
    private String periodo;
}
