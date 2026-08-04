package com.snor.quotaguard.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration for the application caches.
 *
 * <p>The map is keyed by cache name (e.g. {@code users}); adding a cache is a
 * plain {@code application.yml} block under {@code quotaguard.cache.caches}
 * — no code change. Mirrors the {@code RateLimitProperties} pattern and is
 * auto-registered via {@code @ConfigurationPropertiesScan} on
 * {@code QuotaGuardApplication}.</p>
 *
 * @param caches per-cache TTL and maximum-size settings
 */
@ConfigurationProperties(prefix = "quotaguard.cache")
public record CacheProperties(Map<String, CacheSettings> caches) {

    /**
     * Settings for a single cache.
     *
     * @param ttl     time-to-live (expire-after-write) of cached entries
     * @param maxSize maximum number of entries the cache may hold
     */
    public record CacheSettings(Duration ttl, int maxSize) {
    }
}
