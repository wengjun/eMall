package com.emall.cost.domain;

import java.math.BigDecimal;

public record CostSummary(String serviceName, BigDecimal monthlyBudget, BigDecimal currentSpend, String currency,
        int alertThresholdPercent, boolean budgetAlert, int recentSignals, int activeActions) {
}
