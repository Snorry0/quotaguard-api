package com.snor.quotaguard.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide caching infrastructure (Caffeine-backed).
 *
 * <p>Each configured cache is registered with its per-cache TTL
 * (expire-after-write) and maximum size from {@link CacheProperties}, with
 * {@code recordStats()} enabled for the Micrometer cache metrics. Null values
 * are rejected (Spring caches only non-null results). The manager is wrapped
 * in a {@link TransactionAwareCacheManagerProxy} so cache puts/evicts are
 * deferred until after the surrounding transaction commits — after a failed
 * write the rolled-back DB state and the (never-applied) cache mutation stay
 * consistent, so no stale reads can appear.</p>
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final CacheProperties properties;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        properties.caches().forEach((name, settings) ->
                manager.registerCustomCache(name, Caffeine.newBuilder()
                        .maximumSize(settings.maxSize())
                        .expireAfterWrite(settings.ttl())
                        .recordStats()
                        .build()));
        manager.setAllowNullValues(false);
        return new TransactionAwareCacheManagerProxy(manager);
    }
}
