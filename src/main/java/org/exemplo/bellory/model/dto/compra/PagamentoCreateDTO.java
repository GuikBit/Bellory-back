package org.exemplo.bellory.model.dto.compra;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.pagamento.Pagamento;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoCreateDTO {
    private Long cobrancaId;
    private BigDecimal valor;
    private Pagamento.FormaPagamento formaPagamento;
    private String observacoes;
}
