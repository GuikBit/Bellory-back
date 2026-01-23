package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuncionarioMetricasDTO {
    private Long funcionarioId;
    private String nomeFuncionario;
    private String periodo;
    private Long totalAtendimentos;
    private Long atendimentosConcluidos;
    private Long atendimentosCancelados;
    private BigDecimal receitaGerada;
    private Double taxaConclusao;
    private Double mediaAtendimentosPorDia;
    private Double taxaOcupacao;
}
