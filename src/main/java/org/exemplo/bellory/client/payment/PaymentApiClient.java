package org.exemplo.bellory.client.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.client.payment.dto.*;
import org.exemplo.bellory.exception.PaymentApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class PaymentApiClient {

    private final RestClient restClient;
    private final Long companyId;

    public PaymentApiClient(
            @Value("${payment.api.url}") String baseUrl,
            @Value("${payment.api.key}") String apiKey,
            @Value("${payment.api.company-id}") Long companyId,
            @Value("${payment.api.timeout-ms:3000}") int timeoutMs) {

        this.companyId = companyId;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(normalizedBase)
                .defaultHeader("X-API-Key", apiKey)
                .defaultHeader("X-Company-Id", String.valueOf(companyId))
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Long getCompanyId() {
        return companyId;
    }

    public CustomerResponse createCustomer(CreateCustomerRequest req) {
        log.info("Payment API >> POST /api/v1/customers name='{}' document='{}' email='{}'",
                req.getName(), req.getDocument(), req.getEmail());
        log.debug("Payment API >> POST /api/v1/customers body={}", req);
        try {
            CustomerResponse resp = restClient.post()
                    .uri("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /customers", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(CustomerResponse.class);
            log.info("Payment API << POST /api/v1/customers OK id={} name='{}'",
                    resp != null ? resp.getId() : null, resp != null ? resp.getName() : null);
            return resp;
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /customers", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public CustomerResponse getCustomer(Long customerId) {
        try {
            return restClient.get()
                    .uri("/api/v1/customers/{id}", customerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /customers/" + customerId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(CustomerResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /customers/" + customerId, e);
        }
    }

    public void deleteCustomer(Long customerId) {
        try {
            restClient.delete()
                    .uri("/api/v1/customers/{id}", customerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("DELETE /customers/" + customerId, response.getStatusCode(), bodyAsString(response));
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em DELETE /customers/" + customerId, e);
        }
    }

    public SubscriptionResponse createSubscription(CreateSubscriptionRequest req) {
        log.info("Payment API >> POST /api/v1/subscriptions customerId={} planId={} cycle={} externalRef='{}'",
                req.getCustomerId(), req.getPlanId(), req.getCycle(), req.getExternalReference());
        log.debug("Payment API >> POST /api/v1/subscriptions body={}", req);
        try {
            SubscriptionResponse resp = restClient.post()
                    .uri("/api/v1/subscriptions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /subscriptions", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(SubscriptionResponse.class);
            log.info("Payment API << POST /api/v1/subscriptions OK id={} status={}",
                    resp != null ? resp.getId() : null, resp != null ? resp.getStatus() : null);
            return resp;
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /subscriptions", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public SubscriptionResponse getSubscription(Long subscriptionId) {
        try {
            return restClient.get()
                    .uri("/api/v1/subscriptions/{id}", subscriptionId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /subscriptions/" + subscriptionId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(SubscriptionResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /subscriptions/" + subscriptionId, e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public List<ChargeResponse> listChargesBySubscription(Long subscriptionId) {
        try {
            PageResponse<ChargeResponse> page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/subscriptions/{id}/charges")
                            .queryParam("size", 100)
                            .queryParam("sort", "createdAt,desc")
                            .build(subscriptionId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /subscriptions/" + subscriptionId + "/charges", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<ChargeResponse>>() {});
            if (page == null || page.getContent() == null) return Collections.emptyList();
            return page.getContent();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /subscriptions/" + subscriptionId + "/charges", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public List<SubscriptionResponse> listSubscriptionsByCustomer(Long customerId) {
        try {
            PageResponse<SubscriptionResponse> page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/subscriptions")
                            .queryParam("customerId", customerId)
                            .queryParam("size", 50)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /subscriptions?customerId=" + customerId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<SubscriptionResponse>>() {});
            if (page == null || page.getContent() == null) return Collections.emptyList();
            return page.getContent();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /subscriptions customerId=" + customerId, e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public List<ChargeResponse> listChargesByCustomer(Long customerId) {
        try {
            PageResponse<ChargeResponse> page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/charges")
                            .queryParam("customerId", customerId)
                            .queryParam("size", 100)
                            .queryParam("sort", "createdAt,desc")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /charges?customerId=" + customerId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<ChargeResponse>>() {});
            if (page == null || page.getContent() == null) return Collections.emptyList();
            return page.getContent();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /charges customerId=" + customerId, e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public AccessStatusResponse getAccessStatus(Long customerId) {
        try {
            return restClient.get()
                    .uri("/api/v1/customers/{id}/access-status", customerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /customers/" + customerId + "/access-status", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(AccessStatusResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /customers/" + customerId + "/access-status", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public PlanResponse getPlan(Long planId) {
        try {
            return restClient.get()
                    .uri("/api/v1/plans/{id}", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans/" + planId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans/" + planId, e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public PlanResponse getPlanByCodigo(String codigo) {
        try {
            return restClient.get()
                    .uri("/api/v1/plans/codigo/{codigo}", codigo)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans/codigo/" + codigo, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans/codigo/" + codigo, e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public List<PlanLimitDto> getPlanLimits(Long planId) {
        try {
            List<PlanLimitDto> body = restClient.get()
                    .uri("/api/v1/plans/{id}/limits", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans/" + planId + "/limits", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<List<PlanLimitDto>>() {});
            return body != null ? body : Collections.emptyList();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans/" + planId + "/limits", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public List<PlanLimitDto> getPlanFeatures(Long planId) {
        try {
            List<PlanLimitDto> body = restClient.get()
                    .uri("/api/v1/plans/{id}/features", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans/" + planId + "/features", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<List<PlanLimitDto>>() {});
            return body != null ? body : Collections.emptyList();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans/" + planId + "/features", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public PlanLimitCheckResponse checkPlanLimit(Long planId, String key, Integer usage) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder.path("/api/v1/plans/{id}/limits/{key}");
                        if (usage != null) b.queryParam("usage", usage);
                        return b.build(planId, key);
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans/" + planId + "/limits/" + key, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanLimitCheckResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans/" + planId + "/limits/" + key, e);
        }
    }

    private PaymentApiException buildException(String opLabel, HttpStatusCode status, String body) {
        int code = status != null ? status.value() : 0;
        String msg = "Payment API erro em " + opLabel + " (HTTP " + code + ")";
        log.warn("{} - body: {}", msg, body);
        return new PaymentApiException(msg, code, body);
    }

    private static String bodyAsString(org.springframework.http.client.ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Wrapper simples pra desserializar páginas da Payment API (Spring Data Page).
     * Só ficamos com content — o restante (pageable/totalElements) pode ser ignorado.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageResponse<T> {
        private List<T> content;
    }
}
