package com.snor.quotaguard.ratelimit.store;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * Storage abstraction for keyed token buckets.
 *
 * <p>The shape mirrors Bucket4j's {@code ProxyManager<T>} intent — a key
 * plus the bucket configuration yields the bucket — so a Redis-backed
 * implementation (via {@code bucket4j_jdk17-redisson}) is a one-file swap
 * later. The in-memory implementation is {@link InMemoryBucketStore}.</p>
 */
public interface KeyedBucketStore {

    /**
     * Returns the bucket for the given key, creating it with the given
     * bandwidth when it does not exist yet.
     *
     * @param key       the bucket key (e.g. {@code IP:<remoteAddr>} or {@code USER:<userId>})
     * @param bandwidth the token-bucket bandwidth for a newly created bucket
     * @return the (possibly new) bucket
     */
    Bucket getBucket(String key, Bandwidth bandwidth);
}
