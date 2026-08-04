package com.snor.quotaguard.ratelimit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration for the rate-limiting feature.
 *
 * <p>The map is keyed by the request path of each protected endpoint
 * (e.g. {@code /api/v1/auth/login}); adding a new rate-limited endpoint
 * is a plain {@code application.yml} block under
 * {@code quotaguard.rate-limiting.endpoints} — no code change.</p>
 *
 * @param endpoints per-path token-bucket limits
 */
@ConfigurationProperties(prefix = "quotaguard.rate-limiting")
public record RateLimitProperties(Map<String, Limit> endpoints) {

    /**
     * A single token-bucket limit: {@code tokens} capacity refilled over
     * {@code refillPeriod} (greedy refill — the bucket is filled
     * progressively, not in interval bursts).
     *
     * @param tokens       bucket capacity
     * @param refillPeriod period over which the full capacity refills
     */
    public record Limit(int tokens, Duration refillPeriod) {
    }
}
