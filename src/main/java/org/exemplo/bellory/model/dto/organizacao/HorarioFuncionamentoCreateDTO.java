package org.exemplo.bellory.model.dto.organizacao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HorarioFuncionamentoCreateDTO {

    private String diaSemana;
    private Boolean ativo;
    private List<PeriodoFuncionamentoDTO> periodos;
}
