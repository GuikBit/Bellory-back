package org.exemplo.bellory.model.dto.push;

import lombok.*;
import org.exemplo.bellory.model.entity.push.CategoriaNotificacao;
import org.exemplo.bellory.model.entity.push.NotificacaoPush;
import org.exemplo.bellory.model.entity.push.PrioridadeNotificacao;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacaoPushDTO {

    private Long id;
    private String titulo;
    private String descricao;
    private String origem;
    private String detalhe;
    private PrioridadeNotificacao prioridade;
    private CategoriaNotificacao categoria;
    private Boolean lido;
    private String icone;
    private String urlAcao;
    private LocalDateTime dtCadastro;
    private LocalDateTime dtRead;

    public NotificacaoPushDTO(NotificacaoPush entity) {
        this.id = entity.getId();
        this.titulo = entity.getTitulo();
        this.descricao = entity.getDescricao();
        this.origem = entity.getOrigem();
        this.detalhe = entity.getDetalhe();
        this.prioridade = entity.getPrioridade();
        this.categoria = entity.getCategoria();
        this.lido = entity.getLido();
        this.icone = entity.getIcone();
        this.urlAcao = entity.getUrlAcao();
        this.dtCadastro = entity.getDtCadastro();
        this.dtRead = entity.getDtRead();
    }
}
