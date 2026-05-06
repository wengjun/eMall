package com.emall.order.domain;

public enum OrderStatus {
    CREATED,
    PENDING_RETRY,
    PAID,
    CANCELLED,
    CLOSED
}
