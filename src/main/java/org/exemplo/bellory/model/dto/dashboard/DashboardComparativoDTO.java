package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;
import org.exemplo.bellory.model.dto.dashboard.DashboardDTO;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardComparativoDTO {
    private DashboardDTO periodoAtual;
    private DashboardDTO periodoAnterior;
    private List<DashboardDTO.TendenciaDTO> comparativos;
    private String descricaoPeriodos;
}
