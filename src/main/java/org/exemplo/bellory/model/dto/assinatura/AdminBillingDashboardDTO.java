package org.exemplo.bellory.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminBillingDashboardDTO {
    private BigDecimal mrr;
    private long totalAtivas;
    private long totalTrial;
    private long totalVencidas;
    private long totalCanceladas;
    private long totalSuspensas;
    private BigDecimal receitaMesAtual;
}
