package org.exemplo.bellory.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AgendamentoCriadoEvent extends ApplicationEvent {

    private final Long agendamentoId;
    private final Long clienteId;
    private final String nomeCliente;
    private final Long organizacaoId;

    public AgendamentoCriadoEvent(Object source, Long agendamentoId, Long clienteId,
                                   String nomeCliente, Long organizacaoId) {
        super(source);
        this.agendamentoId = agendamentoId;
        this.clienteId = clienteId;
        this.nomeCliente = nomeCliente;
        this.organizacaoId = organizacaoId;
    }
}
