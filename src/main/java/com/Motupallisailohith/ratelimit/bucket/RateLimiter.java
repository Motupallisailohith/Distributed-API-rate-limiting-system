package com.Motupallisailohith.ratelimit.bucket;

public interface RateLimiter {
    /** Try to consume `amount` tokens locally. */
    boolean allow(int bucketId, int amount);

    /** Apply a remote delta (consumption) without capacity check. */
    void applyRemoteDelta(int bucketId, int delta);

    /** 
     * Return the number of tokens remaining for this bucket.
     * For algorithms that don’t track tokens, return -1.
     */
    int getRemainingTokens(int bucketId);

    /** Cleanup any background tasks. */
    void shutdown();
}
