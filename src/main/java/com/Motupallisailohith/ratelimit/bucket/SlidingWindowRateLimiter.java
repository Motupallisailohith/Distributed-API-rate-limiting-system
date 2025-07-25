package com.Motupallisailohith.ratelimit.bucket;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Sliding-Window rate limiter.
 * Tracks timestamps of recent requests in a fixed-size time window.
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    // Per-bucket deque of request timestamps (milliseconds)
    private final Map<Integer, Deque<Long>> windows = new ConcurrentHashMap<>();

    private final int maxRequests;    // max allowed in the window
    private final long windowMs;      // window size in milliseconds

    /**
     * @param maxRequests maximum requests allowed per window
     * @param windowMs    window duration in milliseconds
     */
    public SlidingWindowRateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs     = windowMs;
    }

    @Override
    public boolean allow(int bucketId, int amount) {
        long now = System.currentTimeMillis();
        // Get or create the deque for this bucket
        Deque<Long> deque = windows.computeIfAbsent(bucketId, k -> new LinkedList<>());

        synchronized (deque) {
            // Remove timestamps older than (now - windowMs)
            long cutoff = now - windowMs;
            while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                deque.removeFirst();
            }
            // Check capacity
            if (deque.size() + amount <= maxRequests) {
                // Record this request(s)
                for (int i = 0; i < amount; i++) {
                    deque.addLast(now);
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public void applyRemoteDelta(int bucketId, int delta) {
        // Treat remote deltas same as local requests
        allow(bucketId, delta);
    }
    @Override
    public int getRemainingTokens(int bucketId) {
        // Not supported in sliding window
        return -1;
    }

    @Override
    public void shutdown() {
        // No scheduled task to shut down
        windows.clear();
    }
}
