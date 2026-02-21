package org.exemplo.bellory.service.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.push.PushSubscriptionRequestDTO;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.push.NotificacaoPush;
import org.exemplo.bellory.model.entity.push.PushSubscription;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.push.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PushService pushService;
    private final ObjectMapper objectMapper;

    public PushNotificationService(PushSubscriptionRepository pushSubscriptionRepository,
                                   OrganizacaoRepository organizacaoRepository,
                                   @Nullable PushService pushService,
                                   ObjectMapper objectMapper) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.pushService = pushService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PushSubscription subscribe(PushSubscriptionRequestDTO dto, String userAgent) {
        Long userId = TenantContext.getCurrentUserId();
        String userRole = TenantContext.getCurrentRole();
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        // Se ja existe subscription com este endpoint, atualiza
        PushSubscription subscription = pushSubscriptionRepository
                .findByEndpoint(dto.getEndpoint())
                .orElse(new PushSubscription());

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organizacao nao encontrada"));

        subscription.setUserId(userId);
        subscription.setUserRole(userRole);
        subscription.setOrganizacao(organizacao);
        subscription.setEndpoint(dto.getEndpoint());
        subscription.setP256dh(dto.getKeys().getP256dh());
        subscription.setAuth(dto.getKeys().getAuth());
        subscription.setUserAgent(userAgent);

        return pushSubscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.findByEndpoint(endpoint)
                .ifPresent(pushSubscriptionRepository::delete);
    }

    @Async
    public void sendToUser(Long userId, String userRole, Long orgId, NotificacaoPush notificacao) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository
                .findAllByUserIdAndUserRoleAndOrganizacao_Id(userId, userRole, orgId);

        for (PushSubscription sub : subscriptions) {
            sendPush(sub, notificacao);
        }
    }

    @Async
    public void sendToUsers(List<Long> userIds, String userRole, Long orgId, NotificacaoPush notificacao) {
        for (Long userId : userIds) {
            sendToUser(userId, userRole, orgId, notificacao);
        }
    }

    @Async
    public void sendToRole(String role, Long orgId, NotificacaoPush notificacao) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository
                .findAllByUserRoleAndOrganizacao_Id(role, orgId);

        for (PushSubscription sub : subscriptions) {
            sendPush(sub, notificacao);
        }
    }

    private void sendPush(PushSubscription sub, NotificacaoPush notificacao) {
        if (pushService == null) {
            log.warn("PushService nao configurado. Verifique as chaves VAPID.");
            return;
        }

        try {
            Subscription subscription = new Subscription(
                    sub.getEndpoint(),
                    new Subscription.Keys(sub.getP256dh(), sub.getAuth())
            );

            Map<String, Object> payload = new HashMap<>();
            payload.put("title", notificacao.getTitulo());
            payload.put("body", notificacao.getDescricao());
            payload.put("icon", notificacao.getIcone());
            payload.put("url", notificacao.getUrlAcao());
            payload.put("categoria", notificacao.getCategoria() != null ? notificacao.getCategoria().name() : null);
            payload.put("prioridade", notificacao.getPrioridade() != null ? notificacao.getPrioridade().name() : null);

            String payloadJson = objectMapper.writeValueAsString(payload);

            Notification notification = new Notification(subscription, payloadJson);
            HttpResponse response = pushService.send(notification);

            int statusCode = response.getStatusLine().getStatusCode();

            // 410 Gone = subscription expirada, remover do banco
            if (statusCode == 410 || statusCode == 404) {
                log.info("Subscription expirada ({}), removendo: {}", statusCode, sub.getEndpoint());
                pushSubscriptionRepository.delete(sub);
            } else if (statusCode >= 400) {
                log.warn("Erro ao enviar push (status {}): {}", statusCode, sub.getEndpoint());
            }

        } catch (Exception e) {
            log.error("Erro ao enviar push notification para endpoint {}: {}", sub.getEndpoint(), e.getMessage());
        }
    }
}
