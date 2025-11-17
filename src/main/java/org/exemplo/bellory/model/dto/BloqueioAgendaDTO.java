package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class BloqueioAgendaDTO {
    private LocalDateTime inicioBloqueio;
    private LocalDateTime fimBloqueio;
    private String descricao;
    private String tipoBloqueio;
    private String titulo;

    public BloqueioAgendaDTO(LocalDateTime inicioBloqueio, LocalDateTime fimBloqueio, String descricao, String tipoBloqueio) {
        this.inicioBloqueio = inicioBloqueio;
        this.fimBloqueio = fimBloqueio;
        this.descricao = descricao;
        this.tipoBloqueio = tipoBloqueio;
    }
}
