package com.emall.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentChannelStatement(
        long statementId,
        String channel,
        String channelTradeNo,
        long paymentId,
        BigDecimal amount,
        StatementType statementType,
        Instant occurredAt,
        boolean reconciled,
        Instant createdAt
) {
    public PaymentChannelStatement markReconciled() {
        return new PaymentChannelStatement(statementId, channel, channelTradeNo, paymentId, amount, statementType,
                occurredAt, true, createdAt);
    }
}
