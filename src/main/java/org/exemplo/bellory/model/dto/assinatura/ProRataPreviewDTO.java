package org.exemplo.bellory.model.dto.assinatura;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProRataPreviewDTO {
    private String planoAtualCodigo;
    private String planoAtualNome;
    private String novoPlanoNome;
    private String novoPlanoCodigo;
    private String cicloCobranca;

    private BigDecimal valorAtualProporcional;   // credit from current plan
    private BigDecimal valorNovoProporcional;     // cost of new plan
    private BigDecimal valorProRata;              // difference (positive = charge, negative = credit)

    private long diasRestantesCiclo;
    private long diasTotalCiclo;

    private boolean isUpgrade;                    // true if charge, false if credit
    private String mensagem;                      // human readable message
}
