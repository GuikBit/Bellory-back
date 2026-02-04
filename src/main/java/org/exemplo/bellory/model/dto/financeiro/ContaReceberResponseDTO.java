package org.exemplo.bellory.model.dto.financeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.financeiro.ContaReceber;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContaReceberResponseDTO {
    private Long id;
    private String descricao;
    private String documento;
    private String numeroNota;
    private BigDecimal valor;
    private BigDecimal valorRecebido;
    private BigDecimal valorDesconto;
    private BigDecimal valorJuros;
    private BigDecimal valorMulta;
    private BigDecimal valorTotal;
    private BigDecimal valorRestante;
    private LocalDate dtEmissao;
    private LocalDate dtVencimento;
    private LocalDate dtRecebimento;
    private LocalDate dtCompetencia;
    private String status;
    private String statusDescricao;
    private String formaPagamento;
    private Boolean recorrente;
    private String periodicidade;
    private String periodicidadeDescricao;
    private Integer parcelaAtual;
    private Integer totalParcelas;
    private String observacoes;
    private int diasAtraso;
    private LocalDateTime dtCriacao;

    // Cliente
    private Long clienteId;
    private String clienteNome;

    // Cobrança vinculada
    private Long cobrancaId;

    // Categoria
    private Long categoriaFinanceiraId;
    private String categoriaFinanceiraNome;

    // Centro de Custo
    private Long centroCustoId;
    private String centroCustoNome;

    // Conta Bancária
    private Long contaBancariaId;
    private String contaBancariaNome;

    public ContaReceberResponseDTO(ContaReceber entity) {
        this.id = entity.getId();
        this.descricao = entity.getDescricao();
        this.documento = entity.getDocumento();
        this.numeroNota = entity.getNumeroNota();
        this.valor = entity.getValor();
        this.valorRecebido = entity.getValorRecebido();
        this.valorDesconto = entity.getValorDesconto();
        this.valorJuros = entity.getValorJuros();
        this.valorMulta = entity.getValorMulta();
        this.valorTotal = entity.getValorTotal();
        this.valorRestante = entity.getValorRestante();
        this.dtEmissao = entity.getDtEmissao();
        this.dtVencimento = entity.getDtVencimento();
        this.dtRecebimento = entity.getDtRecebimento();
        this.dtCompetencia = entity.getDtCompetencia();
        this.status = entity.getStatus() != null ? entity.getStatus().name() : null;
        this.statusDescricao = entity.getStatus() != null ? entity.getStatus().getDescricao() : null;
        this.formaPagamento = entity.getFormaPagamento();
        this.recorrente = entity.getRecorrente();
        this.observacoes = entity.getObservacoes();
        this.diasAtraso = entity.getDiasAtraso();
        this.dtCriacao = entity.getDtCriacao();
        this.parcelaAtual = entity.getParcelaAtual();
        this.totalParcelas = entity.getTotalParcelas();

        if (entity.getPeriodicidade() != null) {
            this.periodicidade = entity.getPeriodicidade().name();
            this.periodicidadeDescricao = entity.getPeriodicidade().getDescricao();
        }

        if (entity.getCliente() != null) {
            this.clienteId = entity.getCliente().getId();
            this.clienteNome = entity.getCliente().getNomeCompleto();
        }

        if (entity.getCobranca() != null) {
            this.cobrancaId = entity.getCobranca().getId();
        }

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
    }
}
