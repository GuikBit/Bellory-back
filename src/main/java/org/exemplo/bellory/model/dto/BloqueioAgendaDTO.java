package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.funcionario.BloqueioAgenda;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BloqueioAgendaDTO {
    private Long id;
    private String titulo;
    private LocalDateTime inicioBloqueio;
    private LocalDateTime fimBloqueio;
    private String descricao;
    private String tipoBloqueio;

    public BloqueioAgendaDTO(LocalDateTime inicioBloqueio, LocalDateTime fimBloqueio, String descricao, String tipoBloqueio) {
        this.inicioBloqueio = inicioBloqueio;
        this.fimBloqueio = fimBloqueio;
        this.descricao = descricao;
        this.tipoBloqueio = tipoBloqueio;
    }

    public BloqueioAgendaDTO(BloqueioAgenda bloqueio) {
        this.id = bloqueio.getId();
        this.titulo = bloqueio.getTitulo();
        this.inicioBloqueio = bloqueio.getInicioBloqueio();
        this.fimBloqueio = bloqueio.getFimBloqueio();
        this.descricao = bloqueio.getDescricao();
        this.tipoBloqueio = bloqueio.getTipoBloqueio().name();
    }
}
