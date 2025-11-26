package org.exemplo.bellory.model.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuncionarioTopDTO {
    private Long id;
    private String nome;
    private Long totalAtendimentos;
    private BigDecimal receitaGerada;
}
