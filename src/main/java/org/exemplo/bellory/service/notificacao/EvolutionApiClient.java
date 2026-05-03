package org.exemplo.bellory.service.notificacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente HTTP unico para a Evolution API (envio de WhatsApp).
 *
 * Antes desta classe, tres servicos (NotificacaoSchedulerService, AnamneseWhatsAppService,
 * FilaEsperaDispatchService) duplicavam o POST /message/sendText/{instanceName}.
 * O scheduler ainda tinha a apikey hardcoded - agora todos consomem as mesmas
 * propriedades evolution.api.url e evolution.api.key.
 *
 * Cada nova automacao de mensagem deve usar este client em vez de chamar a
 * Evolution API direto.
 */
@Component
@Slf4j
public class EvolutionApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${evolution.api.url:https://wa.bellory.com.br}")
    private String evolutionApiUrl;

    @Value("${evolution.api.key:}")
    private String evolutionApiKey;

    public EvolutionApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Envia mensagem de texto via Evolution API.
     *
     * @param instanceName nome da instancia conectada (ver app.instance)
     * @param telefone numero ja normalizado com DDI (ex: 5511999999999)
     * @param mensagem texto ja renderizado
     * @return remoteJid e messageId extraidos da resposta (podem ser null se a Evolution
     *         retornar um payload inesperado, mas a chamada foi 2xx)
     * @throws RuntimeException se o status HTTP nao for 2xx
     */
    public SendResult sendText(String instanceName, String telefone, String mensagem) {
        String url = evolutionApiUrl + "/message/sendText/" + instanceName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", evolutionApiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("number", telefone);
        body.put("text", mensagem);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Evolution API retornou " + response.getStatusCode());
        }

        String remoteJid = null;
        String messageId = null;
        try {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode keyNode = responseJson.path("key");
            if (!keyNode.isMissingNode()) {
                remoteJid = keyNode.path("remoteJid").asText(null);
                messageId = keyNode.path("id").asText(null);
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel extrair remoteJid/messageId da resposta Evolution: {}", e.getMessage());
        }

        log.debug("Evolution sendText status={} instance={} telefone={} remoteJid={} messageId={}",
                response.getStatusCode(), instanceName, telefone, remoteJid, messageId);

        return new SendResult(remoteJid, messageId);
    }

    public record SendResult(String remoteJid, String messageId) {}
}
