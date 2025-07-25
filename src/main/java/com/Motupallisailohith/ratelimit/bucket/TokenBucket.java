package com.Motupallisailohith.ratelimit.bucket;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * TokenBucket tracks usage tokens and refills periodically.
 */
public class TokenBucket {
    private static final Logger logger = Logger.getLogger(TokenBucket.class.getName());

    private final int bucketId;
    private final int capacity;
    private final long refillIntervalMs;
    private final AtomicInteger tokens;
    private final ScheduledExecutorService refillScheduler = Executors.newSingleThreadScheduledExecutor();

    public TokenBucket(int bucketId, int capacity, long refillIntervalMs) {
        this.bucketId = bucketId;
        this.capacity = capacity;
        this.refillIntervalMs = refillIntervalMs;
        this.tokens = new AtomicInteger(capacity);
    }

    /** Start the periodic refill task */
    public void startRefillTask() {
        refillScheduler.scheduleAtFixedRate(() -> {
            int before = tokens.getAndSet(capacity);
            logger.fine("Bucket " + bucketId + " refilled from " + before + " to " + capacity);
        }, refillIntervalMs, refillIntervalMs, TimeUnit.MILLISECONDS);
    }

    /** Try to consume tokens; returns true if enough tokens existed */
    public synchronized boolean tryConsume(int amount) {
        int current = tokens.get();
        if (current >= amount) {
            tokens.addAndGet(-amount);
            logger.finer("Bucket " + bucketId + " consumed " + amount + ", remaining=" + tokens.get());
            return true;
        } else {
            logger.finer("Bucket " + bucketId + " insufficient tokens: " + current);
            return false;
        }
    }

    /** Apply a remote delta (increase usage) without checking capacity */
    public synchronized void applyRemoteDelta(int delta) {
        // Equivalent to consuming tokens remotely: subtract delta
        int current = tokens.get();
        tokens.set(Math.max(0, current - delta));
        logger.finer("Bucket " + bucketId + " remote delta " + delta + ", remaining=" + tokens.get());
    }
    /**
     * NEW – expose current token count.
     */
    public int getAvailableTokens() {
        return tokens.get();
    }
    /** Shutdown refill scheduler */
    public void shutdown() {
        refillScheduler.shutdownNow();
    }
}
