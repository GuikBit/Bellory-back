package org.exemplo.bellory.model.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CobrancaResumoDTO {
    private BigDecimal valorTotal;
    private BigDecimal valorPago;
    private BigDecimal valorPendente;
    private Boolean temCobrancaVencida;
    private CobrancaDTO cobrancaSinal;
    private CobrancaDTO cobrancaRestante;
    private CobrancaDTO cobrancaIntegral;
}
