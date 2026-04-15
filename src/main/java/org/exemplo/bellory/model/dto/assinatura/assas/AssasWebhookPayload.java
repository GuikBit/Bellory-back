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
public class AssasWebhookPayload {
    private String event;
    private Payment payment;
    private Subscription subscription;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payment {
        private String id;
        private String subscription;
        private String customer;
        private String billingType;
        private String status;
        private BigDecimal value;
        private String dueDate;
        private String paymentDate;
        private String invoiceUrl;
        private String bankSlipUrl;

        // PIX
        private String encodedImage;
        private String payload;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Subscription {
        private String id;
        private String status;
        private String customer;
        private BigDecimal value;
        private String nextDueDate;
        private String cycle;
        private String billingType;
    }
}
