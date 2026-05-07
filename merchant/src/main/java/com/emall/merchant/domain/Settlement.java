package com.emall.merchant.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Settlement(long settlementId, long merchantId, BigDecimal grossAmount, BigDecimal commissionAmount,
        BigDecimal netAmount, SettlementStatus status, Instant periodStart, Instant periodEnd, Instant createdAt,
        Instant updatedAt) {
    public Settlement pay() {
        return new Settlement(settlementId, merchantId, grossAmount, commissionAmount, netAmount, SettlementStatus.PAID,
                periodStart, periodEnd, createdAt, Instant.now());
    }
}
