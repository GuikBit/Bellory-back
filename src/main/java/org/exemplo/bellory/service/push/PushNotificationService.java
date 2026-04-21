package org.exemplo.bellory.service.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import nl.martijndwars.webpush.Urgency;
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
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

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
        log.info("[PUSH] sendToUser userId={}, role={}, orgId={}, titulo='{}'", userId, userRole, orgId, notificacao.getTitulo());

        List<PushSubscription> subscriptions = pushSubscriptionRepository
                .findAllByUserIdAndUserRoleAndOrganizacao_Id(userId, userRole, orgId);

        if (subscriptions.isEmpty()) {
            log.warn("[PUSH] Nenhuma subscription encontrada para userId={}, role={}, orgId={}. "
                    + "O frontend registrou a subscription via POST /api/v1/push/subscribe?", userId, userRole, orgId);
            return;
        }

        log.info("[PUSH] Encontradas {} subscription(s) para userId={}", subscriptions.size(), userId);

        for (PushSubscription sub : subscriptions) {
            sendPush(sub, notificacao);
        }
    }

    @Async
    public void sendToUsers(List<Long> userIds, String userRole, Long orgId, NotificacaoPush notificacao) {
        log.info("[PUSH] sendToUsers userIds={}, role={}, orgId={}", userIds, userRole, orgId);
        for (Long userId : userIds) {
            List<PushSubscription> subscriptions = pushSubscriptionRepository
                    .findAllByUserIdAndUserRoleAndOrganizacao_Id(userId, userRole, orgId);
            if (subscriptions.isEmpty()) {
                log.warn("[PUSH] Nenhuma subscription para userId={}, role={}, orgId={}", userId, userRole, orgId);
            }
            for (PushSubscription sub : subscriptions) {
                sendPush(sub, notificacao);
            }
        }
    }

    @Async
    @Transactional
    public void sendToRole(String role, Long orgId, NotificacaoPush notificacao) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository
                .findAllByUserRoleAndOrganizacao_Id(role, orgId);

        log.info("[PUSH] sendToRole role={}, orgId={}, subscriptions={}", role, orgId, subscriptions.size());

        for (PushSubscription sub : subscriptions) {
            sendPush(sub, notificacao);
        }
    }

    private void sendPush(PushSubscription sub, NotificacaoPush notificacao) {
        if (pushService == null) {
            log.error("[PUSH] PushService e NULL! Chaves VAPID nao configuradas. Nenhum push sera enviado.");
            return;
        }

        try {
            Subscription subscription = new Subscription(
                    sub.getEndpoint(),
                    new Subscription.Keys(sub.getP256dh(), sub.getAuth())
            );

            Map<String, Object> payload = new HashMap<>();
            payload.put("titulo", notificacao.getTitulo());
            payload.put("descricao", notificacao.getDescricao());
            payload.put("url_acao", notificacao.getUrlAcao());
            payload.put("categoria", notificacao.getCategoria() != null ? notificacao.getCategoria().name() : null);
            payload.put("origem", notificacao.getOrigem());

            String payloadJson = objectMapper.writeValueAsString(payload);

            log.info("[PUSH] Enviando push para endpoint={}... titulo='{}'",
                    sub.getEndpoint().substring(0, Math.min(60, sub.getEndpoint().length())) + "...",
                    notificacao.getTitulo());

            Notification notification = Notification.builder()
                    .endpoint(subscription.endpoint)
                    .userPublicKey(subscription.keys.p256dh)
                    .userAuth(subscription.keys.auth)
                    .payload(payloadJson)
                    .ttl(86400)
                    .urgency(Urgency.HIGH)
                    .build();
            HttpResponse response = pushService.send(notification);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                log.info("[PUSH] Enviado com SUCESSO (status {}) para userId={}, orgId={}",
                        statusCode, sub.getUserId(), sub.getOrganizacao().getId());
            } else if (statusCode == 410 || statusCode == 404) {
                log.warn("[PUSH] Subscription EXPIRADA (status {}), removendo do banco. endpoint={}",
                        statusCode, sub.getEndpoint().substring(0, Math.min(60, sub.getEndpoint().length())));
                pushSubscriptionRepository.delete(sub);
            } else {
                log.error("[PUSH] FALHA ao enviar push (status {}). endpoint={}, response={}",
                        statusCode, sub.getEndpoint().substring(0, Math.min(60, sub.getEndpoint().length())),
                        response.getStatusLine().getReasonPhrase());
            }

        } catch (Exception e) {
            log.error("[PUSH] EXCECAO ao enviar push para endpoint={}", sub.getEndpoint(), e);
        }
    }
}
