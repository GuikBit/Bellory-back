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
public class LancamentoFinanceiroUpdateDTO {
    private Long categoriaFinanceiraId;
    private Long centroCustoId;
    private Long contaBancariaId;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dtLancamento;
    private LocalDate dtCompetencia;
    private String formaPagamento;
    private String documento;
    private String numeroNota;
    private String observacoes;
}
