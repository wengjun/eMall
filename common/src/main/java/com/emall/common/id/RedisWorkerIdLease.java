package com.emall.common.id;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

final class RedisWorkerIdLease implements WorkerIdLease {
    private static final int MAXIMUM_WORKERS = 1024;
    private static final String KEY_PREFIX = "emall:id:worker:";
    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return 0
            """, Long.class);
    private final StringRedisTemplate redisTemplate;
    private final String owner;
    private final Duration leaseTtl;
    private final Duration renewInterval;
    private final long workerId;
    private final ScheduledExecutorService renewalExecutor;
    private final AtomicLong validUntilNanos = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();

    RedisWorkerIdLease(StringRedisTemplate redisTemplate, String owner, SnowflakeIdProperties properties) {
        this.redisTemplate = redisTemplate;
        this.owner = owner;
        this.leaseTtl = properties.getLeaseTtl();
        this.renewInterval = properties.getRenewInterval();
        validateDurations(properties);
        this.workerId = allocate(properties.getWorkerId());
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "snowflake-worker-lease-renewal");
            thread.setDaemon(true);
            return thread;
        };
        this.renewalExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        long intervalMillis = properties.getRenewInterval().toMillis();
        renewalExecutor.scheduleWithFixedDelay(this::renewSafely, intervalMillis, intervalMillis,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public long workerId() {
        return workerId;
    }

    @Override
    public void assertValid() {
        if (closed.get() || System.nanoTime() >= validUntilNanos.get()) {
            throw new IllegalStateException("Snowflake worker lease is no longer valid");
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            renewalExecutor.shutdownNow();
            // Let the key expire instead of releasing it immediately. The cooldown prevents
            // same-millisecond ID reuse.
        }
    }

    private long allocate(Long requestedWorkerId) {
        if (requestedWorkerId != null) {
            if (claim(requestedWorkerId)) {
                return requestedWorkerId;
            }
            throw new IllegalStateException("Configured Snowflake worker ID is already leased: " + requestedWorkerId);
        }
        int start = Math.floorMod(owner.hashCode(), MAXIMUM_WORKERS);
        for (int offset = 0; offset < MAXIMUM_WORKERS; offset++) {
            int candidate = (start + offset) % MAXIMUM_WORKERS;
            if (claim(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No Snowflake worker IDs are available");
    }

    private boolean claim(long candidate) {
        long requestStartedAt = System.nanoTime();
        boolean claimed =
                Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + candidate, owner, leaseTtl));
        if (claimed) {
            markValid(requestStartedAt);
        }
        return claimed;
    }

    private void renewSafely() {
        if (closed.get()) {
            return;
        }
        try {
            long requestStartedAt = System.nanoTime();
            Long renewed = redisTemplate.execute(RENEW_SCRIPT, List.of(KEY_PREFIX + workerId), owner,
                    Long.toString(leaseTtl.toMillis()));
            if (Objects.equals(renewed, 1L)) {
                markValid(requestStartedAt);
            } else {
                validUntilNanos.set(0L);
            }
        } catch (RuntimeException ignored) {
            // The last successful renewal remains valid until its Redis TTL expires.
        }
    }

    private void markValid(long requestStartedAt) {
        validUntilNanos.set(requestStartedAt + leaseTtl.minus(renewInterval).toNanos());
    }

    private void validateDurations(SnowflakeIdProperties properties) {
        if (properties.getLeaseTtl().isNegative() || properties.getLeaseTtl().isZero()
                || properties.getRenewInterval().isNegative() || properties.getRenewInterval().isZero()
                || properties.getRenewInterval().multipliedBy(2).compareTo(properties.getLeaseTtl()) >= 0) {
            throw new IllegalArgumentException(
                    "Worker lease renew interval must be positive and less than half the TTL");
        }
    }
}
