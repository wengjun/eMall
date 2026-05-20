package com.emall.payment.domain;

import com.emall.common.privacy.SensitiveDataMasker;
import com.emall.common.privacy.SensitiveDataType;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentOrder(long paymentId, String requestId, long orderId, long userId, BigDecimal amount,
        String channel, String channelTradeNo, PaymentStatus status, boolean orderConfirmed, Instant createdAt,
        Instant updatedAt) {
    public PaymentOrder succeed(String tradeNo) {
        PaymentStateMachine.requireTransition(status, PaymentStatus.SUCCEEDED);
        return new PaymentOrder(paymentId, requestId, orderId, userId, amount, channel, tradeNo,
                PaymentStatus.SUCCEEDED, orderConfirmed, createdAt, Instant.now());
    }

    public PaymentOrder confirmOrder() {
        return new PaymentOrder(paymentId, requestId, orderId, userId, amount, channel, channelTradeNo, status, true,
                createdAt, Instant.now());
    }

    public PaymentOrder refunding() {
        PaymentStateMachine.requireTransition(status, PaymentStatus.REFUNDING);
        return new PaymentOrder(paymentId, requestId, orderId, userId, amount, channel, channelTradeNo,
                PaymentStatus.REFUNDING, orderConfirmed, createdAt, Instant.now());
    }

    public PaymentOrder refunded() {
        PaymentStateMachine.requireTransition(status, PaymentStatus.REFUNDED);
        return new PaymentOrder(paymentId, requestId, orderId, userId, amount, channel, channelTradeNo,
                PaymentStatus.REFUNDED, orderConfirmed, createdAt, Instant.now());
    }

    @Override
    public String toString() {
        return "PaymentOrder[paymentId=" + paymentId + ", requestId=" + requestId + ", orderId=" + orderId + ", userId="
                + userId + ", amount=" + amount + ", channel=" + channel + ", channelTradeNo="
                + SensitiveDataMasker.mask(SensitiveDataType.PAYMENT_REFERENCE, channelTradeNo) + ", status=" + status
                + ", orderConfirmed=" + orderConfirmed + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
