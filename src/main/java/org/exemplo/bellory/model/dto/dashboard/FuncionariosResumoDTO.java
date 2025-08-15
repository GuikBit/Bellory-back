package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuncionariosResumoDTO {
    private Long totalFuncionarios;
    private Long funcionariosAtivos;
    private Double produtividadeMedia;
    private List<FuncionarioPerformanceDTO> topPerformers;
    private Map<String, Long> agendamentosPorFuncionario;
    private Map<String, BigDecimal> receitaPorFuncionario;
    private Double ocupacaoMediaFuncionarios;
}
