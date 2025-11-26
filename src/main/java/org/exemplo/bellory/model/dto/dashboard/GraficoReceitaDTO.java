package org.exemplo.bellory.model.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para representar dados de receita em gráficos
 * Usado para mostrar receita diária, semanal ou mensal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraficoReceitaDTO {
    private String periodo; // Formato: "dd/MM" para diário ou "MM/yyyy" para mensal
    private BigDecimal receita;
}
