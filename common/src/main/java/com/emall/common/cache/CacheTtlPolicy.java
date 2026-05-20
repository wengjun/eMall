package com.emall.common.cache;

import java.time.Duration;
import java.util.Objects;

public final class CacheTtlPolicy {
    private final Duration baseTtl;
    private final double jitterRatio;

    public CacheTtlPolicy(Duration baseTtl, double jitterRatio) {
        if (baseTtl == null || baseTtl.isZero() || baseTtl.isNegative()) {
            throw new IllegalArgumentException("baseTtl must be positive");
        }
        if (jitterRatio < 0 || jitterRatio > 1) {
            throw new IllegalArgumentException("jitterRatio must be between 0 and 1");
        }
        this.baseTtl = baseTtl;
        this.jitterRatio = jitterRatio;
    }

    public Duration ttlForKey(Object key) {
        if (jitterRatio == 0) {
            return baseTtl;
        }
        long baseMillis = baseTtl.toMillis();
        long jitterMillis = Math.max(1L, Math.round(baseMillis * jitterRatio));
        long offset = Math.floorMod(Objects.hashCode(key), (int) jitterMillis + 1);
        return Duration.ofMillis(baseMillis + offset);
    }
}
