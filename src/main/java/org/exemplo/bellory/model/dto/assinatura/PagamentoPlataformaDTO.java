package org.exemplo.bellory.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagamentoPlataformaDTO {
    private Long id;
    private Long cobrancaId;
    private BigDecimal valor;
    private String status;
    private String formaPagamento;
    private String assasPaymentId;
    private LocalDateTime dtPagamento;
    private LocalDateTime dtCriacao;
}
