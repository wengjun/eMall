package com.emall.order.saga;

public enum OrderSagaStatus {
    RUNNING,
    COMPENSATING,
    COMPENSATED,
    COMPLETED,
    MANUAL_REVIEW
}
