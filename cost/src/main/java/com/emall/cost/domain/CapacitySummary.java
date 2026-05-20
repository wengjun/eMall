package com.emall.cost.domain;

import java.math.BigDecimal;

public record CapacitySummary(String serviceName, int safeQps, int currentQps, BigDecimal qpsWatermark,
        int currentReplicas, int maxReplicas, BigDecimal hpaWatermark, CapacityRiskLevel riskLevel,
        String recommendation, BigDecimal monthlyCost, int activeActions) {
}
