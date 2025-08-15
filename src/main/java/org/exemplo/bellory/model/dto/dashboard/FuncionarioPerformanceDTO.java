package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuncionarioPerformanceDTO {
    private Long id;
    private String nome;
    private Long totalAgendamentos;
    private BigDecimal receitaGerada;
    private Double avaliacaoMedia;
    private Double ocupacao;
    private String status;
}
