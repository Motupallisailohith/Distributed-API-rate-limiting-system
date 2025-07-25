package com.Motupallisailohith.ratelimit.bucket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A Leaky-Bucket rate limiter: 
 * incoming requests “fill” the bucket, 
 * and it “leaks” at a constant rate.
 */
public class LeakyBucketRateLimiter implements RateLimiter {
    private final Map<Integer, LeakyBucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final long leakIntervalMs;

    /**
     * @param capacity       max burst size
     * @param leakIntervalMs how often (ms) to leak one token
     */
    public LeakyBucketRateLimiter(int capacity, long leakIntervalMs) {
        this.capacity      = capacity;
        this.leakIntervalMs = leakIntervalMs;
    }

    @Override
    public boolean allow(int bucketId, int amount) {
        LeakyBucket bucket = buckets.computeIfAbsent(bucketId,
            id -> new LeakyBucket(capacity, leakIntervalMs));
        return bucket.tryAdd(amount);
    }

    @Override
    public void applyRemoteDelta(int bucketId, int delta) {
        // remote “consumption” is same as local addition
        allow(bucketId, delta);
    }

    @Override
    public void shutdown() {
        buckets.values().forEach(LeakyBucket::shutdown);
    }
    @Override
    public int getRemainingTokens(int bucketId) {
        // Not supported in leaky‐bucket
        return -1;
    }


    /**
     * Inner class that manages a single leaky bucket’s state.
     */
    private static class LeakyBucket {
        private final int capacity;
        private volatile int level = 0;
        private final ScheduledExecutorService scheduler;

        LeakyBucket(int capacity, long leakIntervalMs) {
            this.capacity  = capacity;
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            // Schedule one‐by‐one leak at fixed rate
            scheduler.scheduleAtFixedRate(() -> {
                synchronized (this) {
                    if (level > 0) {
                        level--;
                    }
                }
            }, leakIntervalMs, leakIntervalMs, TimeUnit.MILLISECONDS);
        }

        /**
         * Try to add tokens (i.e. record requests). 
         * Returns true if after adding, level ≤ capacity.
         */
        public boolean tryAdd(int tokens) {
            synchronized (this) {
                if (level + tokens > capacity) {
                    return false;  // overflow → reject
                }
                level += tokens;
                return true;
            }
        }

        public void shutdown() {
            scheduler.shutdownNow();
        }
    }
}
