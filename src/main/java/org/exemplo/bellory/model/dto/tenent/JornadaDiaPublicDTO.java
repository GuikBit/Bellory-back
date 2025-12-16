package org.exemplo.bellory.model.dto.tenent;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JornadaDiaPublicDTO {
    private String diaSemana;           // "SEGUNDA", "TERCA", etc.
    private String diaSemanaLabel;      // "Segunda-feira", "Terça-feira", etc.
    private Boolean ativo;
    private List<HorarioTrabalhoPublicDTO> horarios;
}
