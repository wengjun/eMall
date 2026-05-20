package com.emall.payment.domain;

import com.emall.common.privacy.SensitiveDataMasker;
import com.emall.common.privacy.SensitiveDataType;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentChannelStatement(long statementId, String channel, String channelTradeNo, long paymentId,
        BigDecimal amount, StatementType statementType, Instant occurredAt, boolean reconciled, Instant createdAt) {
    public PaymentChannelStatement markReconciled() {
        return new PaymentChannelStatement(statementId, channel, channelTradeNo, paymentId, amount, statementType,
                occurredAt, true, createdAt);
    }

    @Override
    public String toString() {
        return "PaymentChannelStatement[statementId=" + statementId + ", channel=" + channel + ", channelTradeNo="
                + SensitiveDataMasker.mask(SensitiveDataType.PAYMENT_REFERENCE, channelTradeNo) + ", paymentId="
                + paymentId + ", amount=" + amount + ", statementType=" + statementType + ", occurredAt=" + occurredAt
                + ", reconciled=" + reconciled + ", createdAt=" + createdAt + "]";
    }
}
