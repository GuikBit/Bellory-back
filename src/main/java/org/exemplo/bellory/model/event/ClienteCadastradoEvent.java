package org.exemplo.bellory.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClienteCadastradoEvent extends ApplicationEvent {

    private final Long clienteId;
    private final String nomeCliente;
    private final String telefone;
    private final String email;
    private final Long organizacaoId;

    public ClienteCadastradoEvent(Object source, Long clienteId, String nomeCliente,
                                   String telefone, String email, Long organizacaoId) {
        super(source);
        this.clienteId = clienteId;
        this.nomeCliente = nomeCliente;
        this.telefone = telefone;
        this.email = email;
        this.organizacaoId = organizacaoId;
    }
}
