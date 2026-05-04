package org.exemplo.bellory.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Disparado uma unica vez ao final de uma importacao em massa via CSV.
 * Substitui os N {@link ClienteCadastradoEvent} que seriam emitidos
 * pelo fluxo unitario, evitando uma enxurrada de pushes para a equipe.
 */
@Getter
public class ClientesImportadosEvent extends ApplicationEvent {

    private final Long importacaoId;
    private final Long organizacaoId;
    private final String nomeArquivo;
    private final int totalLinhas;
    private final int importados;
    private final int ignorados;

    public ClientesImportadosEvent(Object source,
                                   Long importacaoId,
                                   Long organizacaoId,
                                   String nomeArquivo,
                                   int totalLinhas,
                                   int importados,
                                   int ignorados) {
        super(source);
        this.importacaoId = importacaoId;
        this.organizacaoId = organizacaoId;
        this.nomeArquivo = nomeArquivo;
        this.totalLinhas = totalLinhas;
        this.importados = importados;
        this.ignorados = ignorados;
    }
}
