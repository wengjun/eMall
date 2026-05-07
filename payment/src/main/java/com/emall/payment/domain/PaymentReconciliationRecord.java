package com.emall.payment.domain;

import java.time.Instant;

public record PaymentReconciliationRecord(long recordId, long statementId, long paymentId, String channelTradeNo,
        StatementType statementType, ReconciliationStatus status, String message, Instant createdAt) {
}
