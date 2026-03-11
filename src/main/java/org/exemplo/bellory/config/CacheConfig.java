package org.exemplo.bellory.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_ASSAS_SUBSCRIPTION = "assasSubscription";
    public static final String CACHE_ASSAS_PAYMENTS = "assasPayments";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_ASSAS_SUBSCRIPTION,
                CACHE_ASSAS_PAYMENTS
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }
}
