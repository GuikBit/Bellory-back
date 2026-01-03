package org.exemplo.bellory.model.dto.compra;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StripePaymentIntentDTO {
    private String paymentIntentId;
    private String clientSecret;
    private Long amount;
    private String status;
}
