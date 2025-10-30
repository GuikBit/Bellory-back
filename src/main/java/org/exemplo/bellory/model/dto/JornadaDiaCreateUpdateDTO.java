package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JornadaDiaCreateUpdateDTO {

    private String diaSemana;
    private Boolean ativo;
    private List<HorarioTrabalhoDTO> horarios;
}