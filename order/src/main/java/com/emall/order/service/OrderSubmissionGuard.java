package com.emall.order.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OrderSubmissionGuard {
    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentMap<Long, Counter> counters = new ConcurrentHashMap<>();

    public OrderSubmissionGuard(int maxRequests, Duration window) {
        this(maxRequests, window, Clock.systemUTC());
    }

    OrderSubmissionGuard(int maxRequests, Duration window, Clock clock) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    public void check(long userId) {
        if (maxRequests <= 0) {
            return;
        }
        Instant now = clock.instant();
        Counter counter = counters.compute(userId, (ignored, existing) -> {
            if (existing == null || !now.isBefore(existing.windowStart().plus(window))) {
                return new Counter(now, 1);
            }
            return new Counter(existing.windowStart(), existing.count() + 1);
        });
        if (counter.count() > maxRequests) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "too many order submissions from the same user");
        }
    }

    public static OrderSubmissionGuard noop() {
        return new OrderSubmissionGuard(0, Duration.ofMinutes(1));
    }

    private record Counter(Instant windowStart, int count) {
    }
}
