package org.exemplo.bellory.model.dto.assinatura.assas;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssasSubscriptionRequest {
    private String customer;
    private String billingType;
    private BigDecimal value;
    private String cycle;
    private String nextDueDate;
    private String description;
}
