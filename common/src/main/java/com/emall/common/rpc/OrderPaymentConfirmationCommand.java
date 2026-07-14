package com.emall.common.rpc;

import java.io.Serializable;
import java.math.BigDecimal;

public record OrderPaymentConfirmationCommand(long orderId, long paymentId, BigDecimal paidAmount, String currency,
        String channelTradeNo) implements Serializable {
}
