package org.exemplo.bellory.model.dto.compra;

import lombok.*;
import org.exemplo.bellory.model.entity.pagamento.Pagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PagamentoDTO {
    private Long id;
    private Long cobrancaId;
    private Long clienteId;
    private String clienteNome;
    private BigDecimal valor;
    private Pagamento.FormaPagamento formaPagamento;
    private Pagamento.StatusPagamento statusPagamento;
    private LocalDateTime dtPagamento;
    private String observacoes;
    private String numeroTransacao;

    public PagamentoDTO(Pagamento pagamento) {
        this.id = pagamento.getId();
        this.cobrancaId = pagamento.getCobranca() != null ? pagamento.getCobranca().getId() : null;
        this.clienteId = pagamento.getCliente() != null ? pagamento.getCliente().getId() : null;
        this.clienteNome = pagamento.getCliente() != null ? pagamento.getCliente().getNomeCompleto() : null;
        this.valor = pagamento.getValor();
        this.formaPagamento = pagamento.getFormaPagamento();
        this.statusPagamento = pagamento.getStatusPagamento();
        this.dtPagamento = pagamento.getDtPagamento();
        this.observacoes = pagamento.getObservacoes();
        this.numeroTransacao = pagamento.getNumeroTransacao();
    }
}
