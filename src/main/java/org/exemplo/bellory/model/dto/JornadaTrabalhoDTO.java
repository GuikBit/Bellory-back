package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;

@Getter
@Setter
public class JornadaTrabalhoDTO {
    private String diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private Boolean ativo;

    public JornadaTrabalhoDTO(String diaSemana, LocalTime horaInicio, LocalTime horaFim, Boolean ativo) {
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.ativo = ativo;
    }
}
