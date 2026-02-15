package org.exemplo.bellory.model.dto.organizacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HorarioFuncionamentoResponseDTO {

    private Long id;
    private String diaSemana;
    private String diaSemanaLabel;
    private Boolean ativo;
    private List<PeriodoFuncionamentoDTO> periodos;
}
