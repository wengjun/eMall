package com.emall.order.saga;

public class OrderSagaConcurrencyException extends RuntimeException {
    public OrderSagaConcurrencyException(String requestId, long expectedVersion) {
        super("order saga changed concurrently: requestId=" + requestId + ", expectedVersion=" + expectedVersion);
    }
}
