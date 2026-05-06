package com.emall.payment.domain;

public enum ReconciliationStatus {
    MATCHED,
    PAYMENT_NOT_FOUND,
    TRADE_NO_MISMATCH,
    AMOUNT_MISMATCH,
    STATUS_MISMATCH
}
