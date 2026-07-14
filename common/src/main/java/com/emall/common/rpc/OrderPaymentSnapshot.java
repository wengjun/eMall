package com.emall.common.rpc;

import java.io.Serializable;
import java.math.BigDecimal;

public record OrderPaymentSnapshot(long orderId, long userId, BigDecimal payableAmount, String currency,
        String status) implements Serializable {
}
