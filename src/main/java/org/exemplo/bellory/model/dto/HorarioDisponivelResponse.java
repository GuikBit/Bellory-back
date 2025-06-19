package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioDisponivelResponse {
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private String descricao; // Ex: "08:30 - 09:30"
}
