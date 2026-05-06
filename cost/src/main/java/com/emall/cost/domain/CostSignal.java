package com.emall.cost.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record CostSignal(
        long signalId,
        String serviceName,
        CostSignalType signalType,
        BigDecimal metricValue,
        BigDecimal thresholdValue,
        Instant observedAt,
        Instant createdAt
) {
}
