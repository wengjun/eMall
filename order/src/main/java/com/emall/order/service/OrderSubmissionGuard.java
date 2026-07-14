package com.emall.order.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class OrderSubmissionGuard {
    private static final int MAXIMUM_LOCAL_ENTRIES = 10_000;
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local currentTime = redis.call('TIME')
            local now = (currentTime[1] * 1000) + math.floor(currentTime[2] / 1000)
            local resetAt = tonumber(redis.call('HGET', KEYS[1], 'resetAt') or '0')
            if now >= resetAt then
                resetAt = now + tonumber(ARGV[1])
                redis.call('HSET', KEYS[1], 'count', 1, 'resetAt', resetAt)
                redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[1]) * 2)
                return 1
            end
            local count = redis.call('HINCRBY', KEYS[1], 'count', 1)
            redis.call('PEXPIRE', KEYS[1], math.max(1, resetAt - now + tonumber(ARGV[1])))
            return count
            """, Long.class);

    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final StringRedisTemplate redisTemplate;
    private final boolean failClosed;
    private final ConcurrentMap<Long, Counter> localCounters = new ConcurrentHashMap<>();

    public OrderSubmissionGuard(int maxRequests, Duration window) {
        this(maxRequests, window, Clock.systemUTC(), null, false);
    }

    public OrderSubmissionGuard(int maxRequests, Duration window, StringRedisTemplate redisTemplate,
            boolean failClosed) {
        this(maxRequests, window, Clock.systemUTC(), redisTemplate, failClosed);
    }

    OrderSubmissionGuard(int maxRequests, Duration window, Clock clock) {
        this(maxRequests, window, clock, null, false);
    }

    private OrderSubmissionGuard(int maxRequests, Duration window, Clock clock, StringRedisTemplate redisTemplate,
            boolean failClosed) {
        if (maxRequests < 0 || window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("order submission rate-limit configuration is invalid");
        }
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
        this.redisTemplate = redisTemplate;
        this.failClosed = failClosed;
    }

    public void check(long userId) {
        if (maxRequests <= 0) {
            return;
        }
        long count = redisTemplate == null ? incrementLocal(userId) : incrementDistributed(userId);
        if (count > maxRequests) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "too many order submissions from the same user");
        }
    }

    private long incrementDistributed(long userId) {
        try {
            Long count = redisTemplate.execute(RATE_LIMIT_SCRIPT, List.of("emall:order:submission:{" + userId + "}"),
                    Long.toString(window.toMillis()));
            if (count == null) {
                throw new RedisConnectionFailureException("Redis returned no order rate-limit result");
            }
            return count;
        } catch (RuntimeException ex) {
            if (failClosed) {
                throw new BusinessException(ErrorCode.SYSTEM_BUSY, "order submission protection is unavailable");
            }
            return incrementLocal(userId);
        }
    }

    private long incrementLocal(long userId) {
        Instant now = clock.instant();
        if (!localCounters.containsKey(userId) && localCounters.size() >= MAXIMUM_LOCAL_ENTRIES) {
            localCounters.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().windowStart().plus(window)));
            if (localCounters.size() >= MAXIMUM_LOCAL_ENTRIES) {
                throw new BusinessException(ErrorCode.SYSTEM_BUSY, "local order submission protection is saturated");
            }
        }
        Counter counter = localCounters.compute(userId, (ignored, existing) -> {
            if (existing == null || !now.isBefore(existing.windowStart().plus(window))) {
                return new Counter(now, 1);
            }
            return new Counter(existing.windowStart(), existing.count() + 1);
        });
        return counter.count();
    }

    public static OrderSubmissionGuard noop() {
        return new OrderSubmissionGuard(0, Duration.ofMinutes(1));
    }

    int localEntryCount() {
        return localCounters.size();
    }

    private record Counter(Instant windowStart, int count) {
    }
}
