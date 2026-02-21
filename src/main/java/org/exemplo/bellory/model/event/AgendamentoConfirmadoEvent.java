package org.exemplo.bellory.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class AgendamentoConfirmadoEvent extends ApplicationEvent {

    private final Long agendamentoId;
    private final String nomeCliente;
    private final List<Long> funcionarioIds;
    private final Long organizacaoId;

    public AgendamentoConfirmadoEvent(Object source, Long agendamentoId, String nomeCliente,
                                       List<Long> funcionarioIds, Long organizacaoId) {
        super(source);
        this.agendamentoId = agendamentoId;
        this.nomeCliente = nomeCliente;
        this.funcionarioIds = funcionarioIds;
        this.organizacaoId = organizacaoId;
    }
}
