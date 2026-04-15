package org.exemplo.bellory.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class PagamentoRecebidoEvent extends ApplicationEvent {

    private final Long pagamentoId;
    private final BigDecimal valor;
    private final Long clienteId;
    private final String nomeCliente;
    private final String formaPagamento;
    private final Long organizacaoId;

    public PagamentoRecebidoEvent(Object source, Long pagamentoId, BigDecimal valor,
                                   Long clienteId, String nomeCliente,
                                   String formaPagamento, Long organizacaoId) {
        super(source);
        this.pagamentoId = pagamentoId;
        this.valor = valor;
        this.clienteId = clienteId;
        this.nomeCliente = nomeCliente;
        this.formaPagamento = formaPagamento;
        this.organizacaoId = organizacaoId;
    }
}
