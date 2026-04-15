package org.exemplo.bellory.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class AgendamentoCriadoEvent extends ApplicationEvent {

    private final Long agendamentoId;
    private final Long clienteId;
    private final String nomeCliente;
    private final Long organizacaoId;
    private final LocalDateTime dtAgendamento;
    private final String servicos;
    private final String profissional;
    private final BigDecimal valorTotal;

    public AgendamentoCriadoEvent(Object source, Long agendamentoId, Long clienteId,
                                   String nomeCliente, Long organizacaoId,
                                   LocalDateTime dtAgendamento, String servicos,
                                   String profissional, BigDecimal valorTotal) {
        super(source);
        this.agendamentoId = agendamentoId;
        this.clienteId = clienteId;
        this.nomeCliente = nomeCliente;
        this.organizacaoId = organizacaoId;
        this.dtAgendamento = dtAgendamento;
        this.servicos = servicos;
        this.profissional = profissional;
        this.valorTotal = valorTotal;
    }
}
