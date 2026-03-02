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
public class AssasPaymentResponse {
    private String id;
    private String status;
    private BigDecimal value;
    private String invoiceUrl;
    private String bankSlipUrl;

    // PIX
    private String encodedImage; // QR Code base64
    private String payload;      // Copia e cola
    private String expirationDate;
}
