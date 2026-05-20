package com.emall.common.idempotency;

public enum IdempotencyStatus {
    PROCESSING,
    SUCCEEDED,
    RETRYABLE_FAILED,
    TERMINAL_FAILED
}
