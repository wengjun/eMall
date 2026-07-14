package com.emall.order.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class OrderSubmissionGuardTest {
    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void shouldRejectSingleUserWhenWindowLimitIsExceeded() {
        OrderSubmissionGuard guard = new OrderSubmissionGuard(2, Duration.ofMinutes(1), clock);

        guard.check(70001L);
        guard.check(70001L);

        assertThatThrownBy(() -> guard.check(70001L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("too many order submissions");
    }

    @Test
    void shouldResetCounterAfterWindowExpires() {
        OrderSubmissionGuard guard = new OrderSubmissionGuard(1, Duration.ofMinutes(1), clock);

        guard.check(70001L);
        clock.advance(Duration.ofSeconds(61));

        assertThatCode(() -> guard.check(70001L)).doesNotThrowAnyException();
    }

    @Test
    void shouldEnforceOneGlobalLimitAcrossMultipleServiceInstances() {
        SharedCounterRedisTemplate redisTemplate = new SharedCounterRedisTemplate();
        OrderSubmissionGuard first = new OrderSubmissionGuard(2, Duration.ofMinutes(1), redisTemplate, true);
        OrderSubmissionGuard second = new OrderSubmissionGuard(2, Duration.ofMinutes(1), redisTemplate, true);

        first.check(70001L);
        second.check(70001L);

        assertThatThrownBy(() -> first.check(70001L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("too many order submissions");
        assertThat(redisTemplate.calls).hasValue(3L);
    }

    @Test
    void shouldFailClosedWhenDistributedProtectionIsUnavailable() {
        StringRedisTemplate unavailableRedis = new SharedCounterRedisTemplate() {
            @Override
            public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
                throw new RedisConnectionFailureException("redis unavailable");
            }
        };
        OrderSubmissionGuard guard = new OrderSubmissionGuard(2, Duration.ofMinutes(1), unavailableRedis, true);

        assertThatThrownBy(() -> guard.check(70001L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("protection is unavailable");
    }

    private static class SharedCounterRedisTemplate extends StringRedisTemplate {
        private final AtomicLong calls = new AtomicLong();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            return (T) Long.valueOf(calls.incrementAndGet());
        }
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
