package org.exemplo.bellory.model.dto.assinatura.assas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssasSubscriptionResponse {
    private String id;
    private String status;
    private BigDecimal value;
    private String nextDueDate;
    private String billingType;
    private String cycle;
    private String description;
    private String dateCreated;
    private boolean deleted;
}
