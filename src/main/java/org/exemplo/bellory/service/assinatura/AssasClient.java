package org.exemplo.bellory.service.assinatura;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.assinatura.assas.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class AssasClient {

    private final RestTemplate restTemplate;

    @Value("${assas.api.url:}")
    private String assasApiUrl;

    @Value("${assas.api.key:}")
    private String assasApiKey;

    public AssasClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private boolean isConfigurado() {
        return assasApiKey != null && !assasApiKey.isBlank();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", assasApiKey);
        return headers;
    }

    public AssasCustomerResponse criarCliente(AssasCustomerRequest request) {
        if (!isConfigurado()) {
            log.warn("Assas API key nao configurada. Operacao em modo manual.");
            return null;
        }

        try {
            String url = assasApiUrl + "/v3/customers";
            HttpEntity<AssasCustomerRequest> entity = new HttpEntity<>(request, createHeaders());
            ResponseEntity<AssasCustomerResponse> response = restTemplate.postForEntity(url, entity, AssasCustomerResponse.class);
            log.info("Cliente Assas criado: {}", response.getBody() != null ? response.getBody().getId() : "null");
            return response.getBody();
        } catch (Exception e) {
            log.error("Erro ao criar cliente no Assas: {}", e.getMessage());
            return null;
        }
    }

    public AssasSubscriptionResponse criarAssinatura(AssasSubscriptionRequest request) {
        if (!isConfigurado()) {
            log.warn("Assas API key nao configurada. Operacao em modo manual.");
            return null;
        }

        try {
            String url = assasApiUrl + "/v3/subscriptions";
            HttpEntity<AssasSubscriptionRequest> entity = new HttpEntity<>(request, createHeaders());
            ResponseEntity<AssasSubscriptionResponse> response = restTemplate.postForEntity(url, entity, AssasSubscriptionResponse.class);
            log.info("Assinatura Assas criada: {}", response.getBody() != null ? response.getBody().getId() : "null");
            return response.getBody();
        } catch (Exception e) {
            log.error("Erro ao criar assinatura no Assas: {}", e.getMessage());
            return null;
        }
    }

    public void cancelarAssinatura(String assasSubscriptionId) {
        if (!isConfigurado() || assasSubscriptionId == null) {
            return;
        }

        try {
            String url = assasApiUrl + "/v3/subscriptions/" + assasSubscriptionId;
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("Assinatura Assas cancelada: {}", assasSubscriptionId);
        } catch (Exception e) {
            log.error("Erro ao cancelar assinatura no Assas: {}", e.getMessage());
        }
    }

    public AssasPaymentResponse buscarPagamento(String assasPaymentId) {
        if (!isConfigurado() || assasPaymentId == null) {
            return null;
        }

        try {
            String url = assasApiUrl + "/v3/payments/" + assasPaymentId;
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<AssasPaymentResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, AssasPaymentResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Erro ao buscar pagamento no Assas: {}", e.getMessage());
            return null;
        }
    }
}
