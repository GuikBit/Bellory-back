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
public class LancamentoFinanceiroCreateDTO {
    private Long categoriaFinanceiraId;
    private Long centroCustoId;
    private Long contaBancariaId;
    private Long contaBancariaDestinoId; // Para transferências
    private Long contaPagarId;
    private Long contaReceberId;
    private String tipo; // RECEITA, DESPESA, TRANSFERENCIA
    private String descricao;
    private BigDecimal valor;
    private LocalDate dtLancamento;
    private LocalDate dtCompetencia;
    private String status; // EFETIVADO, PENDENTE
    private String formaPagamento;
    private String documento;
    private String numeroNota;
    private String observacoes;
}
