package com.emall.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRefundOrder(long refundId, long paymentId, String requestId, String channel,
        String channelRefundNo, BigDecimal amount, PaymentRefundStatus status, String reason, Instant createdAt,
        Instant updatedAt) {
    public PaymentRefundOrder processing() {
        return processing(channelRefundNo);
    }

    public PaymentRefundOrder processing(String nextChannelRefundNo) {
        return new PaymentRefundOrder(refundId, paymentId, requestId, channel, nextChannelRefundNo, amount,
                PaymentRefundStatus.PROCESSING, reason, createdAt, Instant.now());
    }

    public PaymentRefundOrder succeeded(String nextChannelRefundNo) {
        return new PaymentRefundOrder(refundId, paymentId, requestId, channel, nextChannelRefundNo, amount,
                PaymentRefundStatus.SUCCEEDED, reason, createdAt, Instant.now());
    }

    public PaymentRefundOrder failed(String message) {
        return new PaymentRefundOrder(refundId, paymentId, requestId, channel, channelRefundNo, amount,
                PaymentRefundStatus.FAILED, message, createdAt, Instant.now());
    }
}
