package com.gianmdp03.cuando_llega_pro.domain.telemetry.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import java.time.Duration;

/**
 * Globally serializes and spaces every request that reaches the MGP proxy.
 */
@Component
public class MgpRequestPacer {

    private final long minimumIntervalNanos;
    private final long defaultRateLimitCooldownNanos;
    private final Semaphore concurrentRequests;
    private long nextRequestNanos;

    @Autowired
    public MgpRequestPacer(
            @Value("${app.proxy.mgp.minimum-interval-ms:6000}") long minimumIntervalMillis,
            @Value("${app.proxy.mgp.maximum-concurrent-requests:1}") int maximumConcurrentRequests,
            @Value("${app.proxy.mgp.rate-limit-cooldown-ms:60000}") long defaultRateLimitCooldownMillis
    ) {
        if (minimumIntervalMillis < 0) {
            throw new IllegalArgumentException("app.proxy.mgp.minimum-interval-ms must not be negative");
        }
        if (maximumConcurrentRequests < 1) {
            throw new IllegalArgumentException("app.proxy.mgp.maximum-concurrent-requests must be at least one");
        }
        if (defaultRateLimitCooldownMillis < 1) {
            throw new IllegalArgumentException("app.proxy.mgp.rate-limit-cooldown-ms must be at least one");
        }
        this.minimumIntervalNanos = minimumIntervalMillis * 1_000_000L;
        this.defaultRateLimitCooldownNanos = defaultRateLimitCooldownMillis * 1_000_000L;
        this.concurrentRequests = new Semaphore(maximumConcurrentRequests, true);
    }

    MgpRequestPacer(long minimumIntervalMillis) {
        this(minimumIntervalMillis, 2, 60_000);
    }

    public MgpRequestPacer(long minimumIntervalMillis, int maximumConcurrentRequests) {
        this(minimumIntervalMillis, maximumConcurrentRequests, 60_000);
    }

    /**
     * Reserves the next request slot. The wait happens outside the monitor so other callers can reserve later slots.
     */
    public void awaitRequestSlot() {
        long waitNanos;
        synchronized (this) {
            long now = System.nanoTime();
            long requestNanos = Math.max(now, nextRequestNanos);
            nextRequestNanos = requestNanos + minimumIntervalNanos;
            waitNanos = requestNanos - now;
        }

        if (waitNanos > 0) {
            try {
                Thread.sleep(Duration.ofNanos(waitNanos));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new MgpRequestInterruptedException(exception);
            }
        }
    }

    /** Executes one upstream request after applying both global pacing and concurrency limits. */
    public <T> T execute(Supplier<T> request) {
        try {
            concurrentRequests.acquire();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MgpRequestInterruptedException(exception);
        }
        try {
            awaitRequestSlot();
            return request.get();
        } finally {
            concurrentRequests.release();
        }
    }

    /** Stops all new MGP requests after a rate limit, honoring Retry-After when supplied. */
    public void registerRateLimit(Duration retryAfter) {
        long requestedCooldownNanos = retryAfter == null || retryAfter.isNegative() || retryAfter.isZero()
                ? defaultRateLimitCooldownNanos
                : retryAfter.toNanos();
        synchronized (this) {
            nextRequestNanos = Math.max(nextRequestNanos, System.nanoTime() + requestedCooldownNanos);
        }
    }
}

class MgpRequestInterruptedException extends RuntimeException {
    MgpRequestInterruptedException(InterruptedException cause) {
        super("Interrupted while waiting for the next MGP request slot", cause);
    }
}
