package com.emall.governance.recovery;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class AdaptiveRecoveryController {
    private final AdaptiveRecoveryPolicy policy;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicInteger consecutiveSuccesses = new AtomicInteger();

    private volatile DownstreamState state = DownstreamState.CLOSED;
    private volatile Instant openedAt = Instant.EPOCH;
    private volatile double allowedTrafficRatio = 1.0D;

    public AdaptiveRecoveryController(AdaptiveRecoveryPolicy policy) {
        this(policy, Clock.systemUTC());
    }

    public AdaptiveRecoveryController(AdaptiveRecoveryPolicy policy, Clock clock) {
        this.policy = policy;
        this.clock = clock;
    }

    public boolean allowRequest() {
        if (state == DownstreamState.OPEN && canProbe()) {
            state = DownstreamState.HALF_OPEN;
            allowedTrafficRatio = policy.halfOpenInitialTrafficRatio();
        }
        if (state == DownstreamState.CLOSED) {
            return true;
        }
        if (state == DownstreamState.OPEN) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < allowedTrafficRatio;
    }

    public void recordSuccess() {
        consecutiveFailures.set(0);
        if (state != DownstreamState.HALF_OPEN) {
            return;
        }
        int successes = consecutiveSuccesses.incrementAndGet();
        if (successes >= policy.successThreshold()) {
            close();
            return;
        }
        allowedTrafficRatio = Math.min(1.0D, allowedTrafficRatio + policy.halfOpenTrafficStep());
    }

    public void recordFailure() {
        consecutiveSuccesses.set(0);
        int failures = consecutiveFailures.incrementAndGet();
        if (state == DownstreamState.HALF_OPEN || failures >= policy.failureThreshold()) {
            open();
        }
    }

    public DownstreamState state() {
        return state;
    }

    public double allowedTrafficRatio() {
        return allowedTrafficRatio;
    }

    private boolean canProbe() {
        return openedAt.plus(policy.openDuration()).isBefore(clock.instant());
    }

    private void open() {
        state = DownstreamState.OPEN;
        openedAt = clock.instant();
        allowedTrafficRatio = 0.0D;
    }

    private void close() {
        state = DownstreamState.CLOSED;
        allowedTrafficRatio = 1.0D;
        consecutiveFailures.set(0);
        consecutiveSuccesses.set(0);
    }
}
