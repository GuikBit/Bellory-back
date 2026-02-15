package org.exemplo.bellory.service.notificacao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class N8nWebhookClient {

    private final WebClient webClient;

    @Value("${bellory.n8n.webhook-url:https://auto.bellory.com.br/webhook/bellory-notificacao}")
    private String webhookUrl;

    @Value("${bellory.n8n.timeout:10}")
    private int timeoutSeconds;

    public N8nWebhookClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public void enviarNotificacao(String instanceName, String telefone,
            String mensagem, Long agendamentoId, String tipoNotificacao) {

        log.debug("Enviando para n8n: instance={}, tel={}", instanceName, telefone);

        Map<String, Object> payload = Map.of(
            "instanceName", instanceName,
            "telefone", telefone,
            "mensagem", mensagem,
            "agendamentoId", agendamentoId,
            "tipoNotificacao", tipoNotificacao,
            "timestamp", System.currentTimeMillis()
        );

        webClient.post()
            .uri(webhookUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .toBodilessEntity()
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .doOnSuccess(r -> log.debug("n8n respondeu: {}", r.getStatusCode()))
            .doOnError(e -> log.error("Erro n8n: {}", e.getMessage()))
            .block();
    }
}
