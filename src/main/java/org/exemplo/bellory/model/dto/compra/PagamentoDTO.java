package org.exemplo.bellory.model.dto.compra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoDTO {
    private Long id;
    private Long cobrancaId;
    private BigDecimal valor;
    private LocalDateTime dataPagamento;
    private String metodoPagamento;
    private String statusPagamento;
    private String transacaoId;
    private String descricaoTransacao;
}
