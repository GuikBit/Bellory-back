package org.exemplo.bellory.model.dto.financeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContaPagarUpdateDTO {
    private Long categoriaFinanceiraId;
    private Long centroCustoId;
    private Long contaBancariaId;
    private String descricao;
    private String fornecedor;
    private String documento;
    private String numeroNota;
    private BigDecimal valor;
    private BigDecimal valorDesconto;
    private LocalDate dtVencimento;
    private LocalDate dtCompetencia;
    private String formaPagamento;
    private String observacoes;
}
