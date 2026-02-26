package org.exemplo.bellory.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ForgotPasswordRateLimiterService {

    private static final int MAX_REQUESTS = 3;

    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    public boolean allowRequest(String clientIp) {
        AtomicInteger count = requestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        return count.incrementAndGet() <= MAX_REQUESTS;
    }

    @Scheduled(fixedRate = 300000)
    public void resetCounts() {
        requestCounts.clear();
    }
}
