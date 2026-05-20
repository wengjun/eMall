package com.emall.common.rpc;

import java.io.Serializable;

public record CouponConfirmationCommand(String reservationId, String couponId, long orderId) implements Serializable {
}
