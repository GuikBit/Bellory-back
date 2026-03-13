package org.exemplo.bellory.model.dto.assinatura.assas;

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
public class AssasPaymentRequest {
    private String customer;
    private String billingType;
    private BigDecimal value;
    private String dueDate;
    private String description;
    private String externalReference;

    // Dados do cartao de credito (enviados ao Asaas, NUNCA armazenados localmente)
    private AssasCreditCardDTO creditCard;
    private AssasCreditCardHolderInfoDTO creditCardHolderInfo;
    private String creditCardToken;
}
