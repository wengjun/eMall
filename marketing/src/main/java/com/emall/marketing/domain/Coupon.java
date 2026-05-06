package com.emall.marketing.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Coupon(
        String couponId,
        long userId,
        BigDecimal thresholdAmount,
        BigDecimal discountAmount,
        CouponStatus status,
        Instant expiresAt,
        Instant updatedAt
) {
    public boolean usable(BigDecimal orderAmount, Instant now) {
        return status == CouponStatus.AVAILABLE
                && !expiresAt.isBefore(now)
                && orderAmount.compareTo(thresholdAmount) >= 0;
    }

    public Coupon used() {
        return new Coupon(couponId, userId, thresholdAmount, discountAmount,
                CouponStatus.USED, expiresAt, Instant.now());
    }
}
