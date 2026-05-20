package com.emall.common.rpc;

import java.io.Serializable;

public record CouponReleaseCommand(String reservationId, String couponId, long orderId) implements Serializable {
}
