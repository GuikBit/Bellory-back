package org.exemplo.bellory.model.dto.financeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.financeiro.LancamentoFinanceiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LancamentoFinanceiroResponseDTO {
    private Long id;
    private String tipo;
    private String tipoDescricao;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dtLancamento;
    private LocalDate dtCompetencia;
    private String status;
    private String statusDescricao;
    private String formaPagamento;
    private String documento;
    private String numeroNota;
    private String observacoes;
    private LocalDateTime dtCriacao;

    // Categoria
    private Long categoriaFinanceiraId;
    private String categoriaFinanceiraNome;

    // Centro de Custo
    private Long centroCustoId;
    private String centroCustoNome;

    // Conta Bancária Origem
    private Long contaBancariaId;
    private String contaBancariaNome;

    // Conta Bancária Destino (transferências)
    private Long contaBancariaDestinoId;
    private String contaBancariaDestinoNome;

    // Vínculos
    private Long contaPagarId;
    private Long contaReceberId;

    public LancamentoFinanceiroResponseDTO(LancamentoFinanceiro entity) {
        this.id = entity.getId();
        this.tipo = entity.getTipo() != null ? entity.getTipo().name() : null;
        this.tipoDescricao = entity.getTipo() != null ? entity.getTipo().getDescricao() : null;
        this.descricao = entity.getDescricao();
        this.valor = entity.getValor();
        this.dtLancamento = entity.getDtLancamento();
        this.dtCompetencia = entity.getDtCompetencia();
        this.status = entity.getStatus() != null ? entity.getStatus().name() : null;
        this.statusDescricao = entity.getStatus() != null ? entity.getStatus().getDescricao() : null;
        this.formaPagamento = entity.getFormaPagamento();
        this.documento = entity.getDocumento();
        this.numeroNota = entity.getNumeroNota();
        this.observacoes = entity.getObservacoes();
        this.dtCriacao = entity.getDtCriacao();

        if (entity.getCategoriaFinanceira() != null) {
            this.categoriaFinanceiraId = entity.getCategoriaFinanceira().getId();
            this.categoriaFinanceiraNome = entity.getCategoriaFinanceira().getNome();
        }

        if (entity.getCentroCusto() != null) {
            this.centroCustoId = entity.getCentroCusto().getId();
            this.centroCustoNome = entity.getCentroCusto().getNome();
        }

        if (entity.getContaBancaria() != null) {
            this.contaBancariaId = entity.getContaBancaria().getId();
            this.contaBancariaNome = entity.getContaBancaria().getNome();
        }

        if (entity.getContaBancariaDestino() != null) {
            this.contaBancariaDestinoId = entity.getContaBancariaDestino().getId();
            this.contaBancariaDestinoNome = entity.getContaBancariaDestino().getNome();
        }

        if (entity.getContaPagar() != null) {
            this.contaPagarId = entity.getContaPagar().getId();
        }

        if (entity.getContaReceber() != null) {
            this.contaReceberId = entity.getContaReceber().getId();
        }
    }
}
