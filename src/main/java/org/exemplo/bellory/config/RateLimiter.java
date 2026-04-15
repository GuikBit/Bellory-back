package org.exemplo.bellory.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter in-memory para endpoints públicos.
 * Limita requisições por chave (IP, telefone, etc.) em janelas de tempo.
 */
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, RateEntry> entries = new ConcurrentHashMap<>();

    /**
     * Verifica se a chave pode fazer a requisição.
     *
     * @param key       Chave de identificação (ex: "ip:192.168.1.1" ou "tel:11999998888")
     * @param maxRequests Máximo de requisições permitidas na janela
     * @param windowMinutes Janela de tempo em minutos
     * @return true se permitido, false se excedeu o limite
     */
    public boolean tryAcquire(String key, int maxRequests, int windowMinutes) {
        LocalDateTime now = LocalDateTime.now();
        RateEntry entry = entries.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStart.plusMinutes(windowMinutes).isBefore(now)) {
                return new RateEntry(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return entry.count.get() <= maxRequests;
    }

    /**
     * Limpa entradas expiradas a cada 5 minutos.
     */
    @Scheduled(fixedRate = 300000)
    public void cleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        entries.entrySet().removeIf(e -> e.getValue().windowStart.isBefore(threshold));
    }

    private static class RateEntry {
        final LocalDateTime windowStart;
        final AtomicInteger count;

        RateEntry(LocalDateTime windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
