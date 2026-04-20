package org.exemplo.bellory.model.repository.webhook;

import org.exemplo.bellory.model.entity.webhook.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {
    Optional<WebhookConfig> findFirstByAtivoTrue();
}
