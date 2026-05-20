package com.emall.common.rpc;

import java.io.Serializable;
import java.math.BigDecimal;

public record CouponReservationCommand(String reservationId, long userId, String couponId, BigDecimal orderAmount,
        long orderId) implements Serializable {
}
