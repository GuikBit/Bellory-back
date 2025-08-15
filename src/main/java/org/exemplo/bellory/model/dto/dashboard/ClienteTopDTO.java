package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteTopDTO {
    private Long id;
    private String nome;
    private Long totalAgendamentos;
    private BigDecimal valorGasto;
    private LocalDateTime ultimoAgendamento;
}
