package com.Motupallisailohith.ratelimit.bucket;



import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-Bucket based RateLimiter.
 * Each key (API token hash) gets its own TokenBucket,
 * which refills on a fixed interval.
 */
public class TokenBucketRateLimiter implements RateLimiter {
    // Active buckets keyed by bucketId
    private final Map<Integer, TokenBucket> buckets = new ConcurrentHashMap<>();

    // Default config — can be made configurable via constructor overload
    private final int defaultCapacity;        // tokens per interval
    private final long defaultRefillMs;       // refill interval in milliseconds

    /**
     * Initialize with default capacity=100 tokens/minute.
     */
    public TokenBucketRateLimiter() {
        this(100, 60_000L);
    }

    /**
     * Initialize with custom capacity & refill interval.
     *
     * @param capacity     max tokens per bucket
     * @param refillMs     refill interval (milliseconds)
     */
    public TokenBucketRateLimiter(int capacity, long refillMs) {
        this.defaultCapacity    = capacity;
        this.defaultRefillMs    = refillMs;
    }

    /**
     * Fetch or create a TokenBucket for the given key.
     */
    private TokenBucket getBucket(int bucketId) {
        return buckets.computeIfAbsent(bucketId, id -> {
            TokenBucket tb = new TokenBucket(id, defaultCapacity, defaultRefillMs);
            tb.startRefillTask();
            return tb;
        });
    }

    /**
     * Try to consume `amount` tokens. Returns true if allowed, false if rate-limited.
     */
    @Override
    public boolean allow(int bucketId, int amount) {
        TokenBucket tb = getBucket(bucketId);
        return tb.tryConsume(amount);
    }

    /**
     * Apply a remote delta (treat it like local consumption, without capacity check).
     */
    @Override
    public void applyRemoteDelta(int bucketId, int delta) {
        TokenBucket tb = getBucket(bucketId);
        tb.applyRemoteDelta(delta);
    }
    @Override
    public int getRemainingTokens(int bucketId) {
        // Expose the current available tokens
        TokenBucket bucket = getBucket(bucketId);
        return bucket.getAvailableTokens();
    }

    /**
     * Cleanly shut down all TokenBucket refill tasks.
     */
    @Override
    public void shutdown() {
        buckets.values().forEach(TokenBucket::shutdown);
    }
}
