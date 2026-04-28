package org.exemplo.bellory.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Disparado quando um agendamento e adiantado via fila de espera (cliente
 * aceitou a oferta). Listeners de push notificam admin e funcionarios.
 */
@Getter
public class AgendamentoAdiantadoFilaEvent extends ApplicationEvent {

    private final Long agendamentoId;
    private final Long clienteId;
    private final String nomeCliente;
    private final List<Long> funcionarioIds;
    private final Long organizacaoId;
    private final LocalDateTime dtAgendamentoOriginal;
    private final LocalDateTime dtAgendamentoNova;
    private final String servicos;
    private final String profissional;

    public AgendamentoAdiantadoFilaEvent(Object source,
                                         Long agendamentoId,
                                         Long clienteId,
                                         String nomeCliente,
                                         List<Long> funcionarioIds,
                                         Long organizacaoId,
                                         LocalDateTime dtAgendamentoOriginal,
                                         LocalDateTime dtAgendamentoNova,
                                         String servicos,
                                         String profissional) {
        super(source);
        this.agendamentoId = agendamentoId;
        this.clienteId = clienteId;
        this.nomeCliente = nomeCliente;
        this.funcionarioIds = funcionarioIds;
        this.organizacaoId = organizacaoId;
        this.dtAgendamentoOriginal = dtAgendamentoOriginal;
        this.dtAgendamentoNova = dtAgendamentoNova;
        this.servicos = servicos;
        this.profissional = profissional;
    }
}
