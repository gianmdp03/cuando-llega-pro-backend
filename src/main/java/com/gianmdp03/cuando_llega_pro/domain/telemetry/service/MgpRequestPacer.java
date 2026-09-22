package com.gianmdp03.cuando_llega_pro.domain.telemetry.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

/**
 * Globally spaces live MGP arrival requests so concurrent users cannot create a burst against the upstream API.
 */
@Component
public class MgpRequestPacer {

    private final long minimumIntervalNanos;
    private final Semaphore concurrentRequests;
    private long nextRequestNanos;

    @Autowired
    public MgpRequestPacer(
            @Value("${app.proxy.arrivals.minimum-interval-ms:400}") long minimumIntervalMillis,
            @Value("${app.proxy.arrivals.maximum-concurrent-requests:2}") int maximumConcurrentRequests
    ) {
        if (minimumIntervalMillis < 0) {
            throw new IllegalArgumentException("app.proxy.arrivals.minimum-interval-ms must not be negative");
        }
        if (maximumConcurrentRequests < 1) {
            throw new IllegalArgumentException("app.proxy.arrivals.maximum-concurrent-requests must be at least one");
        }
        this.minimumIntervalNanos = minimumIntervalMillis * 1_000_000L;
        this.concurrentRequests = new Semaphore(maximumConcurrentRequests);
    }

    MgpRequestPacer(long minimumIntervalMillis) {
        this(minimumIntervalMillis, 2);
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
            LockSupport.parkNanos(waitNanos);
        }
    }

    /** Executes one upstream request after applying both global pacing and concurrency limits. */
    public <T> T execute(Supplier<T> request) {
        concurrentRequests.acquireUninterruptibly();
        try {
            awaitRequestSlot();
            return request.get();
        } finally {
            concurrentRequests.release();
        }
    }
}
