package org.exemplo.bellory.service.assinatura;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.client.payment.PaymentApiClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Executa compensações assíncronas na Payment API — usado quando uma operação
 * composta (ex.: signup) falha parcialmente e deixa recursos remotos órfãos.
 *
 * Se as compensações falharem mesmo após retries, o log de ERROR é a pista para
 * limpeza manual do customer/subscription órfão na Payment API.
 */
@Slf4j
@Service
public class PaymentApiCompensationService {

    private final PaymentApiClient client;

    public PaymentApiCompensationService(PaymentApiClient client) {
        this.client = client;
    }

    @Async
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 5000, multiplier = 2))
    public void deleteCustomerSafe(Long customerId) {
        if (customerId == null) return;
        try {
            client.deleteCustomer(customerId);
            log.info("Compensacao OK: deleteCustomer paymentApiCustomerId={}", customerId);
        } catch (Exception e) {
            log.error("Compensacao FALHOU (tentativa sera retryada): deleteCustomer paymentApiCustomerId={}: {}",
                    customerId, e.getMessage());
            throw e;
        }
    }
}
