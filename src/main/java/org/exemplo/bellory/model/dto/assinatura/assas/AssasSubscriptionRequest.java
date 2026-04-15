package org.exemplo.bellory.model.dto.assinatura.assas;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssasSubscriptionRequest {
    private String customer;
    private String billingType;
    private BigDecimal value;
    private String cycle;
    private String nextDueDate;
    private String description;

    // Dados do cartao de credito (enviados ao Asaas, NUNCA armazenados localmente)
    private AssasCreditCardDTO creditCard;
    private AssasCreditCardHolderInfoDTO creditCardHolderInfo;

    // Token do cartao (alternativa ao envio dos dados completos)
    private String creditCardToken;
}
