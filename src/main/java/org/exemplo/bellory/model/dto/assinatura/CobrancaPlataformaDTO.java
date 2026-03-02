package org.exemplo.bellory.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CobrancaPlataformaDTO {
    private Long id;
    private BigDecimal valor;
    private LocalDate dtVencimento;
    private LocalDateTime dtPagamento;
    private String status;
    private String formaPagamento;
    private String assasInvoiceUrl;
    private String assasBankSlipUrl;
    private String assasPixQrCode;
    private String assasPixCopiaCola;
    private Integer referenciaMes;
    private Integer referenciaAno;
    private LocalDateTime dtCriacao;
}
