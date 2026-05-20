package com.emall.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentLedgerEntry(long ledgerId, long paymentId, long orderId, long userId, LedgerDirection direction,
        String accountCode, BigDecimal amount, String currency, String businessType, String referenceId,
        Instant createdAt) {
}
