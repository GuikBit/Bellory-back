package org.exemplo.bellory.model.repository.assinatura;

import org.exemplo.bellory.model.entity.assinatura.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {

    boolean existsByAssasPaymentIdAndEvento(String assasPaymentId, String evento);
}
