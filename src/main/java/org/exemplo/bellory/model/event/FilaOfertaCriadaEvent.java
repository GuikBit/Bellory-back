package org.exemplo.bellory.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Disparado quando uma {@code FilaEsperaTentativa} e salva (status=PENDENTE).
 * O dispatch via WhatsApp acontece num listener AFTER_COMMIT async, para que a
 * chamada externa nao prenda a transacao principal.
 */
@Getter
public class FilaOfertaCriadaEvent extends ApplicationEvent {

    private final Long tentativaId;
    private final Long organizacaoId;

    public FilaOfertaCriadaEvent(Object source, Long tentativaId, Long organizacaoId) {
        super(source);
        this.tentativaId = tentativaId;
        this.organizacaoId = organizacaoId;
    }
}
