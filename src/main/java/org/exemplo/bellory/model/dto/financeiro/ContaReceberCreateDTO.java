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
public class ContaReceberCreateDTO {
    private Long categoriaFinanceiraId;
    private Long centroCustoId;
    private Long contaBancariaId;
    private Long clienteId;
    private String descricao;
    private String documento;
    private String numeroNota;
    private BigDecimal valor;
    private BigDecimal valorDesconto;
    private LocalDate dtEmissao;
    private LocalDate dtVencimento;
    private LocalDate dtCompetencia;
    private String formaPagamento;
    private Boolean recorrente;
    private String periodicidade;
    private Integer totalParcelas;
    private String observacoes;
}
