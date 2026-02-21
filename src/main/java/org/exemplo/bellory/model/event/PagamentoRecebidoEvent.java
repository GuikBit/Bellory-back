package org.exemplo.bellory.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class PagamentoRecebidoEvent extends ApplicationEvent {

    private final Long pagamentoId;
    private final BigDecimal valor;
    private final String nomeCliente;
    private final Long organizacaoId;

    public PagamentoRecebidoEvent(Object source, Long pagamentoId, BigDecimal valor,
                                   String nomeCliente, Long organizacaoId) {
        super(source);
        this.pagamentoId = pagamentoId;
        this.valor = valor;
        this.nomeCliente = nomeCliente;
        this.organizacaoId = organizacaoId;
    }
}
