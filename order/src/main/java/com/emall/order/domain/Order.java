package com.emall.order.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Order(long orderId, String requestId, long userId, long skuId, int quantity, OrderClientType clientType,
        String deviceId, String channel, BigDecimal unitPrice, BigDecimal subtotalAmount, BigDecimal discountAmount,
        BigDecimal payableAmount, String currency, long priceVersion, String couponId, String inventoryReservationId,
        OrderStatus status, String failureReason, Instant createdAt, Instant updatedAt) {
    public Order markPaid() {
        return withStatus(OrderStatus.PAID, null);
    }

    public Order markCancelled() {
        return withStatus(OrderStatus.CANCELLED, null);
    }

    public Order markCreated() {
        return withStatus(OrderStatus.CREATED, null);
    }

    public Order markPendingRetry(String reason) {
        return withStatus(OrderStatus.PENDING_RETRY, reason);
    }

    private Order withStatus(OrderStatus newStatus, String newFailureReason) {
        OrderStateMachine.requireTransition(status, newStatus);
        return new Order(orderId, requestId, userId, skuId, quantity, clientType, deviceId, channel, unitPrice,
                subtotalAmount, discountAmount, payableAmount, currency, priceVersion, couponId, inventoryReservationId,
                newStatus, newFailureReason, createdAt, Instant.now());
    }
}
