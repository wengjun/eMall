package com.emall.marketing.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Coupon(String couponId, long userId, BigDecimal thresholdAmount, BigDecimal discountAmount,
        CouponStatus status, Instant expiresAt, String reservationId, long reservedOrderId, Instant updatedAt) {
    public Coupon(String couponId, long userId, BigDecimal thresholdAmount, BigDecimal discountAmount,
            CouponStatus status, Instant expiresAt, Instant updatedAt) {
        this(couponId, userId, thresholdAmount, discountAmount, status, expiresAt, null, 0L, updatedAt);
    }

    public boolean usable(BigDecimal orderAmount, Instant now) {
        return status == CouponStatus.AVAILABLE && !expiresAt.isBefore(now)
                && orderAmount.compareTo(thresholdAmount) >= 0;
    }

    public boolean reservedBy(String expectedReservationId, long expectedOrderId) {
        return status == CouponStatus.RESERVED && reservationMatches(expectedReservationId, expectedOrderId);
    }

    public boolean reservationMatches(String expectedReservationId, long expectedOrderId) {
        return reservationId != null && reservationId.equals(expectedReservationId)
                && reservedOrderId == expectedOrderId;
    }

    public Coupon reserved(String nextReservationId, long nextOrderId) {
        return new Coupon(couponId, userId, thresholdAmount, discountAmount, CouponStatus.RESERVED, expiresAt,
                nextReservationId, nextOrderId, Instant.now());
    }

    public Coupon used() {
        return new Coupon(couponId, userId, thresholdAmount, discountAmount, CouponStatus.USED, expiresAt,
                reservationId, reservedOrderId, Instant.now());
    }

    public Coupon released() {
        return new Coupon(couponId, userId, thresholdAmount, discountAmount, CouponStatus.AVAILABLE, expiresAt, null,
                0L, Instant.now());
    }
}
