package com.emall.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentOrder(
        long paymentId,
        String requestId,
        long orderId,
        long userId,
        BigDecimal amount,
        String channel,
        String channelTradeNo,
        PaymentStatus status,
        boolean orderConfirmed,
        Instant createdAt,
        Instant updatedAt
) {
    public PaymentOrder succeed(String tradeNo) {
        return new PaymentOrder(paymentId, requestId, orderId, userId, amount, channel, tradeNo,
                PaymentStatus.SUCCEEDED, orderConfirmed, createdAt, Instant.now());
    }

    public PaymentOrder confirmOrder() {
        return new PaymentOrder(paymentId, requestId, orderId, userId, amount, channel, channelTradeNo,
                status, true, createdAt, Instant.now());
    }

    public PaymentOrder refunding() {
        return new PaymentOrder(paymentId, requestId, orderId, userId, amount, channel, channelTradeNo,
                PaymentStatus.REFUNDING, orderConfirmed, createdAt, Instant.now());
    }

    public PaymentOrder refunded() {
        return new PaymentOrder(paymentId, requestId, orderId, userId, amount, channel, channelTradeNo,
                PaymentStatus.REFUNDED, orderConfirmed, createdAt, Instant.now());
    }
}
