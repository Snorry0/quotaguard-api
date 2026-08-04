package com.snor.quotaguard.ratelimit.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * In-memory {@link KeyedBucketStore} backed by a bounded Caffeine cache.
 *
 * <p>The cache bounds the number of live buckets ({@value #MAX_BUCKETS}) and
 * evicts idle ones (10 minutes without access) so a bucket per IP/user never
 * grows unboundedly. Bucket4j local buckets are thread-safe; the Caffeine
 * lookup is the single creation path — a bucket is never rebuilt per
 * request.</p>
 */
@Component
public class InMemoryBucketStore implements KeyedBucketStore {

    private static final int MAX_BUCKETS = 10_000;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(MAX_BUCKETS)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    @Override
    public Bucket getBucket(String key, Bandwidth bandwidth) {
        return buckets.get(key, k -> Bucket.builder().addLimit(bandwidth).build());
    }
}
