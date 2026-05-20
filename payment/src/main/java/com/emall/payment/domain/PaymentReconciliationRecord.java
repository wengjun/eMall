package com.emall.payment.domain;

import com.emall.common.privacy.SensitiveDataMasker;
import com.emall.common.privacy.SensitiveDataType;
import java.time.Instant;

public record PaymentReconciliationRecord(long recordId, long statementId, long paymentId, String channelTradeNo,
        StatementType statementType, ReconciliationStatus status, String message, Instant createdAt) {
    @Override
    public String toString() {
        return "PaymentReconciliationRecord[recordId=" + recordId + ", statementId=" + statementId + ", paymentId="
                + paymentId + ", channelTradeNo="
                + SensitiveDataMasker.mask(SensitiveDataType.PAYMENT_REFERENCE, channelTradeNo) + ", statementType="
                + statementType + ", status=" + status + ", message=" + message + ", createdAt=" + createdAt + "]";
    }
}
