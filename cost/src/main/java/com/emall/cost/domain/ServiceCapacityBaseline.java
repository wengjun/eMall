package com.emall.cost.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record ServiceCapacityBaseline(long baselineId, String serviceName, int safeQps, int peakQps, int currentQps,
        int currentReplicas, int maxReplicas, BigDecimal cpuUtilization, BigDecimal memoryUtilization,
        BigDecimal monthlyCost, boolean sloProtected, CapacityRiskLevel riskLevel, String recommendation,
        Instant observedAt, Instant createdAt) {
    public BigDecimal qpsWatermark() {
        return safeQps <= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(currentQps).divide(BigDecimal.valueOf(safeQps), 6, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal hpaWatermark() {
        return maxReplicas <= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(currentReplicas).divide(BigDecimal.valueOf(maxReplicas), 6,
                        java.math.RoundingMode.HALF_UP);
    }
}
