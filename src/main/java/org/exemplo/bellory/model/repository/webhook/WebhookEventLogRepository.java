package org.exemplo.bellory.model.repository.webhook;

import org.exemplo.bellory.model.entity.webhook.WebhookEventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookEventLogRepository extends JpaRepository<WebhookEventLog, Long> {
    boolean existsByEventId(String eventId);
    Page<WebhookEventLog> findAllByOrderByDtRecebidoDesc(Pageable pageable);
    Page<WebhookEventLog> findByEventTypeOrderByDtRecebidoDesc(String eventType, Pageable pageable);
    Page<WebhookEventLog> findByOrganizacaoIdOrderByDtRecebidoDesc(Long organizacaoId, Pageable pageable);
}
