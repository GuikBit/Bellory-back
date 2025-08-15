package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendamentoRecenteDTO {
    private Long id;
    private String clienteNome;
    private String funcionarioNome;
    private String servicoNome;
    private LocalDateTime dataHora;
    private String status;
    private BigDecimal valor;
}
