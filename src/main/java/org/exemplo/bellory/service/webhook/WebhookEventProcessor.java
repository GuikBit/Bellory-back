package org.exemplo.bellory.service.webhook;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.webhook.WebhookEnvelopeDTO;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.push.CategoriaNotificacao;
import org.exemplo.bellory.model.entity.push.PrioridadeNotificacao;
import org.exemplo.bellory.model.entity.webhook.WebhookEventConfig;
import org.exemplo.bellory.model.entity.webhook.WebhookEventLog;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.webhook.WebhookEventConfigRepository;
import org.exemplo.bellory.model.repository.webhook.WebhookEventLogRepository;
import org.exemplo.bellory.service.EmailService;
import org.exemplo.bellory.service.assinatura.AssinaturaCacheService;
import org.exemplo.bellory.service.push.NotificacaoPushService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class WebhookEventProcessor {

    private final WebhookEventConfigRepository eventConfigRepository;
    private final WebhookEventLogRepository eventLogRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final NotificacaoPushService notificacaoPushService;
    private final EmailService emailService;
    private final AssinaturaCacheService assinaturaCacheService;

    public WebhookEventProcessor(WebhookEventConfigRepository eventConfigRepository,
                                 WebhookEventLogRepository eventLogRepository,
                                 OrganizacaoRepository organizacaoRepository,
                                 NotificacaoPushService notificacaoPushService,
                                 EmailService emailService,
                                 AssinaturaCacheService assinaturaCacheService) {
        this.eventConfigRepository = eventConfigRepository;
        this.eventLogRepository = eventLogRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.notificacaoPushService = notificacaoPushService;
        this.emailService = emailService;
        this.assinaturaCacheService = assinaturaCacheService;
    }

    @Async
    public void processarAsync(Long eventLogId, WebhookEnvelopeDTO envelope, Long organizacaoId) {
        try {
            processar(eventLogId, envelope, organizacaoId);
        } catch (Exception e) {
            log.error("Webhook: erro ao processar evento {} (type={}): {}",
                    envelope.getId(), envelope.getType(), e.getMessage(), e);
            atualizarStatus(eventLogId, "FAILED", e.getMessage());
        }
    }

    private void processar(Long eventLogId, WebhookEnvelopeDTO envelope, Long organizacaoId) {
        atualizarStatus(eventLogId, "PROCESSING", null);

        // Buscar config do evento
        Optional<WebhookEventConfig> configOpt = eventConfigRepository.findByEventType(envelope.getType());
        if (configOpt.isEmpty() || !configOpt.get().isAtivo()) {
            log.info("Webhook: evento {} sem config ativa, ignorando acoes", envelope.getType());
            atualizarStatus(eventLogId, "PROCESSED", null);
            return;
        }

        WebhookEventConfig config = configOpt.get();
        Map<String, Object> data = envelope.getData();

        // 1. Invalidar cache se configurado
        if (config.isInvalidarCache() && organizacaoId != null) {
            try {
                assinaturaCacheService.refreshByOrganizacao(organizacaoId);
                log.info("Webhook: cache invalidado para org={} (evento={})", organizacaoId, envelope.getType());
            } catch (Exception e) {
                log.warn("Webhook: falha ao invalidar cache org={}: {}", organizacaoId, e.getMessage());
            }
        }

        // 2. Enviar push para admins da organizacao
        if (config.isPushEnabled() && organizacaoId != null) {
            enviarPushNotificacao(envelope, data, organizacaoId);
        }

        // 3. Enviar email para emailPrincipal da organizacao
        if (config.isEmailEnabled() && organizacaoId != null) {
            enviarEmail(envelope, data, organizacaoId);
        }

        atualizarStatus(eventLogId, "PROCESSED", null);
    }

    private void enviarPushNotificacao(WebhookEnvelopeDTO envelope, Map<String, Object> data, Long organizacaoId) {
        try {
            String titulo = buildTituloPush(envelope.getType(), data);
            String descricao = buildDescricaoPush(envelope.getType(), data);
            PrioridadeNotificacao prioridade = getPrioridade(envelope.getType());

            notificacaoPushService.criarEEnviarParaRole(
                    "ROLE_ADMIN", organizacaoId,
                    titulo, descricao, "Payment API",
                    CategoriaNotificacao.PAGAMENTO, prioridade,
                    CategoriaNotificacao.PAGAMENTO.getIcone(), null,
                    envelope.getType(), null
            );
            log.info("Webhook: push enviado para admins org={} (evento={})", organizacaoId, envelope.getType());
        } catch (Exception e) {
            log.warn("Webhook: falha ao enviar push org={}: {}", organizacaoId, e.getMessage());
        }
    }

    private void enviarEmail(WebhookEnvelopeDTO envelope, Map<String, Object> data, Long organizacaoId) {
        try {
            Optional<Organizacao> orgOpt = organizacaoRepository.findById(organizacaoId);
            if (orgOpt.isEmpty() || orgOpt.get().getEmailPrincipal() == null) {
                log.warn("Webhook: org {} sem emailPrincipal, email nao enviado", organizacaoId);
                return;
            }

            String email = orgOpt.get().getEmailPrincipal();
            String nomeOrg = orgOpt.get().getNomeFantasia();
            String assunto = buildAssuntoEmail(envelope.getType(), data, nomeOrg);
            String corpo = buildCorpoEmail(envelope.getType(), data, nomeOrg);

            emailService.enviarEmailSimples(email, assunto, corpo);
            log.info("Webhook: email enviado para {} (evento={})", email, envelope.getType());
        } catch (Exception e) {
            log.warn("Webhook: falha ao enviar email org={}: {}", organizacaoId, e.getMessage());
        }
    }

    // ==================== BUILDERS ====================

    private String buildTituloPush(String eventType, Map<String, Object> data) {
        String valor = formatarValor(data);
        return switch (eventType) {
            case "ChargeCreatedEvent" -> "Nova cobrança gerada" + valor;
            case "ChargePaidEvent" -> "Pagamento confirmado" + valor;
            case "ChargeCanceledEvent" -> "Cobrança cancelada" + valor;
            case "ChargeRefundedEvent" -> "Estorno realizado" + valor;
            case "SubscriptionCreatedEvent" -> "Assinatura criada";
            case "SubscriptionPausedEvent" -> "Assinatura pausada";
            case "SubscriptionResumedEvent" -> "Assinatura retomada";
            case "SubscriptionSuspendedEvent" -> "Assinatura suspensa";
            case "SubscriptionCanceledEvent" -> "Assinatura cancelada";
            case "PlanChangeScheduledEvent" -> "Troca de plano agendada";
            case "PlanChangePendingPaymentEvent" -> "Troca de plano aguardando pagamento";
            case "PlanChangedEvent" -> "Plano alterado" + formatarPlano(data);
            default -> "Evento: " + eventType;
        };
    }

    private String buildDescricaoPush(String eventType, Map<String, Object> data) {
        String valor = formatarValor(data);
        return switch (eventType) {
            case "ChargeCreatedEvent" -> "Uma nova cobrança" + valor + " foi gerada para sua assinatura.";
            case "ChargePaidEvent" -> "O pagamento" + valor + " foi confirmado com sucesso.";
            case "ChargeCanceledEvent" -> "A cobrança" + valor + " foi cancelada.";
            case "ChargeRefundedEvent" -> "Um estorno" + valor + " foi realizado.";
            case "SubscriptionSuspendedEvent" -> "Sua assinatura foi suspensa por inadimplência. Regularize para manter o acesso.";
            case "SubscriptionCanceledEvent" -> "Sua assinatura foi cancelada definitivamente.";
            case "PlanChangedEvent" -> "Seu plano foi alterado" + formatarPlano(data) + " com sucesso.";
            default -> "Evento " + eventType + " recebido da Payment API.";
        };
    }

    private String buildAssuntoEmail(String eventType, Map<String, Object> data, String nomeOrg) {
        String valor = formatarValor(data);
        return switch (eventType) {
            case "ChargeCreatedEvent" -> "Bellory — Nova cobrança gerada" + valor;
            case "ChargePaidEvent" -> "Bellory — Pagamento confirmado" + valor;
            case "ChargeRefundedEvent" -> "Bellory — Estorno realizado" + valor;
            case "SubscriptionPausedEvent" -> "Bellory — Assinatura pausada";
            case "SubscriptionResumedEvent" -> "Bellory — Assinatura retomada";
            case "SubscriptionSuspendedEvent" -> "Bellory — Atenção: assinatura suspensa";
            case "SubscriptionCanceledEvent" -> "Bellory — Assinatura cancelada";
            case "PlanChangedEvent" -> "Bellory — Plano alterado com sucesso";
            default -> "Bellory — Notificação de pagamento";
        };
    }

    private String buildCorpoEmail(String eventType, Map<String, Object> data, String nomeOrg) {
        String valor = formatarValor(data);
        String saudacao = "Olá, " + nomeOrg + "!\n\n";
        String rodape = "\n\nAtenciosamente,\nEquipe Bellory";

        return switch (eventType) {
            case "ChargeCreatedEvent" -> saudacao +
                    "Uma nova cobrança" + valor + " foi gerada para sua assinatura.\n" +
                    "Acesse o painel para visualizar os detalhes e efetuar o pagamento." + rodape;
            case "ChargePaidEvent" -> saudacao +
                    "Seu pagamento" + valor + " foi confirmado com sucesso!\n" +
                    "Obrigado por manter sua assinatura em dia." + rodape;
            case "ChargeRefundedEvent" -> saudacao +
                    "Um estorno" + valor + " foi realizado na sua conta." + rodape;
            case "SubscriptionSuspendedEvent" -> saudacao +
                    "Sua assinatura foi suspensa por inadimplência.\n" +
                    "Regularize o pagamento para manter o acesso ao sistema." + rodape;
            case "SubscriptionCanceledEvent" -> saudacao +
                    "Sua assinatura foi cancelada.\n" +
                    "Caso deseje reativar, entre em contato com nosso suporte." + rodape;
            case "PlanChangedEvent" -> saudacao +
                    "Seu plano foi alterado" + formatarPlano(data) + " com sucesso!\n" +
                    "As novas funcionalidades já estão disponíveis." + rodape;
            default -> saudacao +
                    "Recebemos uma atualização sobre sua assinatura.\n" +
                    "Acesse o painel para mais detalhes." + rodape;
        };
    }

    private PrioridadeNotificacao getPrioridade(String eventType) {
        return switch (eventType) {
            case "SubscriptionSuspendedEvent", "SubscriptionCanceledEvent" -> PrioridadeNotificacao.ALTA;
            case "ChargePaidEvent", "PlanChangedEvent" -> PrioridadeNotificacao.MEDIA;
            default -> PrioridadeNotificacao.BAIXA;
        };
    }

    private String formatarValor(Map<String, Object> data) {
        if (data == null) return "";
        Object value = data.get("value");
        if (value == null) value = data.get("effectivePrice");
        if (value == null) return "";
        try {
            BigDecimal v = new BigDecimal(value.toString());
            return " R$ " + String.format("%.2f", v);
        } catch (Exception e) {
            return "";
        }
    }

    private String formatarPlano(Map<String, Object> data) {
        if (data == null) return "";
        Object planName = data.get("requestedPlanName");
        if (planName == null) planName = data.get("planName");
        if (planName == null) return "";
        return " para " + planName;
    }

    private void atualizarStatus(Long eventLogId, String status, String errorMessage) {
        try {
            Optional<WebhookEventLog> logOpt = eventLogRepository.findById(eventLogId);
            if (logOpt.isPresent()) {
                WebhookEventLog eventLog = logOpt.get();
                eventLog.setStatus(status);
                eventLog.setErrorMessage(errorMessage);
                if ("PROCESSED".equals(status) || "FAILED".equals(status)) {
                    eventLog.setDtProcessado(LocalDateTime.now());
                }
                eventLogRepository.save(eventLog);
            }
        } catch (Exception e) {
            log.error("Webhook: falha ao atualizar status do log {}: {}", eventLogId, e.getMessage());
        }
    }
}
