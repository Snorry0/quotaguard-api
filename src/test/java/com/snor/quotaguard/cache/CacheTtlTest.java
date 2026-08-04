package com.snor.quotaguard.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Caffeine semantics the production cache config relies on
 * (the {@code CacheConfig} registers every cache with {@code expireAfterWrite}
 * + {@code maximumSize} + {@code recordStats}).
 *
 * <p>The TTL values mirror the production yml ({@code quotaguard.cache.caches.users}
 * = 5m TTL / 1000 max-size). The TTL test is deterministic via the hand-rolled
 * {@link TestTicker} — no sleeps, no {@code caffeine-testlib} (Gate 0 F3).
 */
class CacheTtlTest {

    @Test
    void expiresAfterWriteTtl() {
        TestTicker ticker = new TestTicker();
        Cache<String, String> cache = Caffeine.newBuilder()
                .ticker(ticker::read)
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1_000)
                .build();

        cache.put("a@x.com", "value");
        assertThat(cache.getIfPresent("a@x.com")).isEqualTo("value"); // hit

        ticker.advance(Duration.ofMinutes(6));
        assertThat(cache.getIfPresent("a@x.com")).isNull(); // expired -> miss (enforced on read)
    }

    @Test
    void evictsWhenRemoved() {
        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1_000)
                .build();

        cache.put("a@x.com", "value");
        assertThat(cache.getIfPresent("a@x.com")).isEqualTo("value");

        cache.invalidate("a@x.com");
        assertThat(cache.getIfPresent("a@x.com")).isNull(); // explicit eviction semantics
    }

    @Test
    void boundedByMaxSize() {
        Cache<Integer, String> cache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .build();

        for (int i = 0; i < 1_001; i++) {
            cache.put(i, "value-" + i);
        }
        cache.cleanUp(); // run the pending maintenance so the size-based eviction applies
        assertThat(cache.estimatedSize()).isLessThanOrEqualTo(1_000); // the oldest entries evicted
    }

    /**
     * Hand-rolled deterministic clock for the TTL test (Gate 0 F3 — NO
     * {@code caffeine-testlib}; the {@code TestTimeMeter} precedent from rate limiting).
     */
    private static class TestTicker implements com.github.benmanes.caffeine.cache.Ticker {

        private long nanos;

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }

        @Override
        public long read() {
            return nanos;
        }
    }
}
