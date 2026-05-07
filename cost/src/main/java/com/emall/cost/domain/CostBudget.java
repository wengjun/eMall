package com.emall.cost.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record CostBudget(long budgetId, String serviceName, BigDecimal monthlyBudget, BigDecimal currentSpend,
        String currency, int alertThresholdPercent, boolean active, Instant createdAt, Instant updatedAt) {
    public CostBudget update(BigDecimal monthlyBudget, BigDecimal currentSpend, String currency,
            int alertThresholdPercent, boolean active) {
        return new CostBudget(budgetId, serviceName, monthlyBudget, currentSpend, currency, alertThresholdPercent,
                active, createdAt, Instant.now());
    }

    public boolean overAlertThreshold() {
        BigDecimal threshold = monthlyBudget.multiply(BigDecimal.valueOf(alertThresholdPercent))
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        return currentSpend.compareTo(threshold) >= 0;
    }
}
