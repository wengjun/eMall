package com.emall.common.rpc;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record CouponReservationView(String reservationId, long userId, String couponId, String status,
        BigDecimal discountAmount, long orderId, Instant updatedAt) implements Serializable {
}
