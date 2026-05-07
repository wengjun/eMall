package com.emall.merchant.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Invoice(long invoiceId, long settlementId, long merchantId, BigDecimal amount, InvoiceStatus status,
        String invoiceTitle, Instant createdAt, Instant updatedAt) {
    public Invoice sent() {
        return new Invoice(invoiceId, settlementId, merchantId, amount, InvoiceStatus.SENT, invoiceTitle, createdAt,
                Instant.now());
    }
}
