package com.emall.merchant.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record CommissionRule(
        long ruleId,
        long merchantId,
        BigDecimal rate,
        boolean active,
        Instant effectiveFrom,
        Instant createdAt,
        Instant updatedAt
) {
}
