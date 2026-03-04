package org.exemplo.bellory.model.dto.cupom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.assinatura.CupomDesconto;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CupomValidacaoResult {
    private boolean valido;
    private String mensagem;
    private CupomDesconto cupom;
    private BigDecimal valorOriginal;
    private BigDecimal valorDesconto;
    private BigDecimal valorComDesconto;
}
