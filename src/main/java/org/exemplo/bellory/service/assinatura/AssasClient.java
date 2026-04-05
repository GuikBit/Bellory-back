package org.exemplo.bellory.service.assinatura;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.config.CacheConfig;
import org.exemplo.bellory.exception.AssasApiException;
import org.exemplo.bellory.model.dto.assinatura.assas.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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

    public boolean isConfigurado() {
        return assasApiKey != null && !assasApiKey.isBlank();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", assasApiKey);
        return headers;
    }

    private void verificarConfiguracao() {
        if (!isConfigurado()) {
            throw new AssasApiException("Asaas API key não configurada");
        }
    }

    // ==================== CUSTOMERS ====================

    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AssasCustomerResponse criarCliente(AssasCustomerRequest request) {
        verificarConfiguracao();
        try {
            String url = assasApiUrl + "/v3/customers";
            HttpEntity<AssasCustomerRequest> entity = new HttpEntity<>(request, createHeaders());
            ResponseEntity<AssasCustomerResponse> response = restTemplate.postForEntity(url, entity, AssasCustomerResponse.class);
            log.info("Cliente Asaas criado: {}", response.getBody() != null ? response.getBody().getId() : "null");
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Erro ao criar cliente no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Falha ao criar cliente no Asaas", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AssasCustomerResponse atualizarCliente(String assasCustomerId, AssasCustomerRequest request) {
        verificarConfiguracao();
        if (assasCustomerId == null) {
            throw new AssasApiException("Customer ID nulo");
        }
        try {
            String url = assasApiUrl + "/v3/customers/" + assasCustomerId;
            HttpEntity<AssasCustomerRequest> entity = new HttpEntity<>(request, createHeaders());
            ResponseEntity<AssasCustomerResponse> response = restTemplate.exchange(url, HttpMethod.PUT, entity, AssasCustomerResponse.class);
            log.info("Cliente Asaas atualizado: {}", assasCustomerId);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Erro ao atualizar cliente no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Falha ao atualizar cliente", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AssasCustomerResponse buscarCliente(String assasCustomerId) {
        verificarConfiguracao();
        if (assasCustomerId == null) return null;
        try {
            String url = assasApiUrl + "/v3/customers/" + assasCustomerId;
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<AssasCustomerResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, AssasCustomerResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Erro ao buscar cliente no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Falha ao buscar cliente", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    // ==================== SUBSCRIPTIONS ====================

    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @CacheEvict(value = CacheConfig.CACHE_ASSAS_SUBSCRIPTION, key = "#result?.id", condition = "#result != null")
    public AssasSubscriptionResponse criarAssinatura(AssasSubscriptionRequest request) {
        verificarConfiguracao();
        try {
            String url = assasApiUrl + "/v3/subscriptions";
            HttpEntity<AssasSubscriptionRequest> entity = new HttpEntity<>(request, createHeaders());
            ResponseEntity<AssasSubscriptionResponse> response = restTemplate.postForEntity(url, entity, AssasSubscriptionResponse.class);
            log.info("Assinatura Asaas criada: {}", response.getBody() != null ? response.getBody().getId() : "null");
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Erro ao criar assinatura no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Falha ao criar assinatura", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @CacheEvict(value = CacheConfig.CACHE_ASSAS_SUBSCRIPTION, key = "#assasSubscriptionId")
    public AssasSubscriptionResponse atualizarAssinatura(String assasSubscriptionId, AssasSubscriptionRequest request) {
        verificarConfiguracao();
        if (assasSubscriptionId == null) {
            throw new AssasApiException("Subscription ID nulo");
        }
        try {
            String url = assasApiUrl + "/v3/subscriptions/" + assasSubscriptionId;
            HttpEntity<AssasSubscriptionRequest> entity = new HttpEntity<>(request, createHeaders());
            ResponseEntity<AssasSubscriptionResponse> response = restTemplate.exchange(url, HttpMethod.PUT, entity, AssasSubscriptionResponse.class);
            log.info("Assinatura Asaas atualizada: {}", assasSubscriptionId);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Erro ao atualizar assinatura no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Falha ao atualizar assinatura", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    @Cacheable(value = CacheConfig.CACHE_ASSAS_SUBSCRIPTION, key = "#assasSubscriptionId", unless = "#result == null")
    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AssasSubscriptionResponse buscarAssinatura(String assasSubscriptionId) {
        verificarConfiguracao();
        if (assasSubscriptionId == null) return null;
        try {
            String url = assasApiUrl + "/v3/subscriptions/" + assasSubscriptionId;
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<AssasSubscriptionResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, AssasSubscriptionResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Assinatura nao encontrada no Asaas: {}", assasSubscriptionId);
            return null;
        } catch (HttpClientErrorException e) {
            log.error("Erro ao buscar assinatura no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Falha ao buscar assinatura", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    @CacheEvict(value = {CacheConfig.CACHE_ASSAS_SUBSCRIPTION, CacheConfig.CACHE_ASSAS_PAYMENTS}, key = "#assasSubscriptionId")
    public void cancelarAssinatura(String assasSubscriptionId) {
        verificarConfiguracao();
        if (assasSubscriptionId == null) return;
        try {
            String url = assasApiUrl + "/v3/subscriptions/" + assasSubscriptionId;
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("Assinatura Asaas cancelada: {}", assasSubscriptionId);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Assinatura ja removida no Asaas: {}", assasSubscriptionId);
        } catch (HttpClientErrorException e) {
            log.error("Erro ao cancelar assinatura no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Falha ao cancelar assinatura", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    // ==================== PAYMENTS ====================

    @Cacheable(value = CacheConfig.CACHE_ASSAS_PAYMENTS, key = "#assasSubscriptionId", unless = "#result == null")
    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AssasPaymentListResponse buscarPagamentosAssinatura(String assasSubscriptionId) {
        verificarConfiguracao();
        if (assasSubscriptionId == null) return null;
        try {
            String url = assasApiUrl + "/v3/payments?subscription=" + assasSubscriptionId + "&limit=50&order=desc";
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<AssasPaymentListResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, AssasPaymentListResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Erro ao buscar pagamentos no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Falha ao buscar pagamentos", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AssasPaymentResponse buscarPagamento(String assasPaymentId) {
        verificarConfiguracao();
        if (assasPaymentId == null) return null;
        try {
            String url = assasApiUrl + "/v3/payments/" + assasPaymentId;
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<AssasPaymentResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, AssasPaymentResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Pagamento nao encontrado no Asaas: {}", assasPaymentId);
            return null;
        } catch (HttpClientErrorException e) {
            log.error("Erro ao buscar pagamento no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Falha ao buscar pagamento", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AssasPaymentResponse estornarPagamento(String assasPaymentId) {
        verificarConfiguracao();
        if (assasPaymentId == null) {
            throw new AssasApiException("Payment ID nulo");
        }
        try {
            String url = assasApiUrl + "/v3/payments/" + assasPaymentId + "/refund";
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<AssasPaymentResponse> response = restTemplate.postForEntity(url, entity, AssasPaymentResponse.class);
            log.info("Pagamento estornado no Asaas: {}", assasPaymentId);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Erro ao estornar pagamento no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Falha ao estornar pagamento", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    // ==================== PIX QR CODE ====================

    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AssasPixQrCodeResponse buscarPixQrCode(String assasPaymentId) {
        verificarConfiguracao();
        if (assasPaymentId == null) return null;
        try {
            String url = assasApiUrl + "/v3/payments/" + assasPaymentId + "/pixQrCode";
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<AssasPixQrCodeResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, AssasPixQrCodeResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("PIX QR Code nao disponivel para pagamento: {}", assasPaymentId);
            return null;
        } catch (HttpClientErrorException e) {
            log.warn("Erro ao buscar PIX QR Code no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        }
    }

    // ==================== ONE-TIME PAYMENTS ====================

    /**
     * Creates a one-time payment for pro-rata charges (convenience overload).
     */
    public AssasPaymentResponse criarCobrancaAvulsa(String customerId, java.math.BigDecimal valor, String billingType, String description) {
        AssasPaymentRequest request = AssasPaymentRequest.builder()
                .customer(customerId)
                .billingType(billingType)
                .value(valor)
                .dueDate(java.time.LocalDate.now().plusDays(1).toString())
                .description(description)
                .build();
        return criarCobrancaAvulsa(request);
    }

    /**
     * Creates a one-time payment (not subscription) in Asaas for pro-rata charges.
     */
    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AssasPaymentResponse criarCobrancaAvulsa(AssasPaymentRequest request) {
        verificarConfiguracao();
        String url = assasApiUrl + "/v3/payments";
        HttpEntity<AssasPaymentRequest> entity = new HttpEntity<>(request, createHeaders());
        try {
            ResponseEntity<AssasPaymentResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, AssasPaymentResponse.class);
            log.info("Cobranca avulsa criada no Asaas: {}", response.getBody() != null ? response.getBody().getId() : "null");
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Erro ao criar cobranca avulsa no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Erro ao criar cobranca avulsa: " + e.getMessage(), e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    /**
     * Fetches payments by status from Asaas (e.g., OVERDUE).
     * Used for batch inadimplence checking instead of N+1 queries.
     */
    @Retryable(retryFor = {HttpServerErrorException.class, RestClientException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AssasPaymentListResponse buscarPagamentosPorStatus(String status) {
        verificarConfiguracao();
        String url = assasApiUrl + "/v3/payments?status=" + status + "&limit=100";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        try {
            ResponseEntity<AssasPaymentListResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AssasPaymentListResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Erro ao buscar pagamentos por status no Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AssasApiException("Erro ao buscar pagamentos: " + e.getMessage(), e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }

    // ==================== CACHE EVICTION ====================

    @CacheEvict(value = CacheConfig.CACHE_ASSAS_SUBSCRIPTION, key = "#assasSubscriptionId")
    public void evictSubscriptionCache(String assasSubscriptionId) {
        log.debug("Cache de subscription evictado: {}", assasSubscriptionId);
    }

    @CacheEvict(value = CacheConfig.CACHE_ASSAS_PAYMENTS, key = "#assasSubscriptionId")
    public void evictPaymentsCache(String assasSubscriptionId) {
        log.debug("Cache de payments evictado: {}", assasSubscriptionId);
    }
}
