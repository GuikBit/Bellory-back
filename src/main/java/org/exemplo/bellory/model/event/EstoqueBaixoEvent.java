package org.exemplo.bellory.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EstoqueBaixoEvent extends ApplicationEvent {

    private final Long produtoId;
    private final String nomeProduto;
    private final int quantidadeAtual;
    private final Long organizacaoId;

    public EstoqueBaixoEvent(Object source, Long produtoId, String nomeProduto,
                              int quantidadeAtual, Long organizacaoId) {
        super(source);
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.quantidadeAtual = quantidadeAtual;
        this.organizacaoId = organizacaoId;
    }
}
