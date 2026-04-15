package org.exemplo.bellory.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AgendamentoCanceladoEvent extends ApplicationEvent {

    private final Long agendamentoId;
    private final Long clienteId;
    private final String nomeCliente;
    private final List<Long> funcionarioIds;
    private final Long organizacaoId;
    private final LocalDateTime dtAgendamento;
    private final String servicos;
    private final String profissional;

    public AgendamentoCanceladoEvent(Object source, Long agendamentoId, Long clienteId,
                                      String nomeCliente, List<Long> funcionarioIds,
                                      Long organizacaoId, LocalDateTime dtAgendamento,
                                      String servicos, String profissional) {
        super(source);
        this.agendamentoId = agendamentoId;
        this.clienteId = clienteId;
        this.nomeCliente = nomeCliente;
        this.funcionarioIds = funcionarioIds;
        this.organizacaoId = organizacaoId;
        this.dtAgendamento = dtAgendamento;
        this.servicos = servicos;
        this.profissional = profissional;
    }
}
