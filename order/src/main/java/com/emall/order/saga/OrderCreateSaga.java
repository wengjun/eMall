package com.emall.order.saga;

import java.time.Instant;

public record OrderCreateSaga(long sagaId, String requestId, long orderId, long userId, long skuId, String couponId,
        String inventoryReservationId, OrderSagaStage stage, OrderSagaStatus status, int attempts, long version,
        String lastError, Instant nextRetryAt, Instant createdAt, Instant updatedAt) {
    public static OrderCreateSaga start(long sagaId, String requestId, long orderId, long userId, long skuId,
            Instant now) {
        return new OrderCreateSaga(sagaId, requestId, orderId, userId, skuId, null, requestId, OrderSagaStage.STARTED,
                OrderSagaStatus.RUNNING, 0, 0L, null, now, now, now);
    }

    public OrderCreateSaga advance(OrderSagaStage nextStage, String nextCouponId, String nextReservationId) {
        return new OrderCreateSaga(sagaId, requestId, orderId, userId, skuId,
                nextCouponId == null ? couponId : nextCouponId,
                nextReservationId == null ? inventoryReservationId : nextReservationId, nextStage,
                OrderSagaStatus.RUNNING, attempts, version + 1, null, nextRetryAt, createdAt, Instant.now());
    }

    public OrderCreateSaga status(OrderSagaStatus nextStatus, String error, Instant retryAt) {
        int nextAttempts = nextStatus == OrderSagaStatus.COMPENSATING ? attempts + 1 : attempts;
        return new OrderCreateSaga(sagaId, requestId, orderId, userId, skuId, couponId, inventoryReservationId, stage,
                nextStatus, nextAttempts, version + 1, error, retryAt, createdAt, Instant.now());
    }

    public OrderCreateSaga restart(Instant now) {
        return new OrderCreateSaga(sagaId, requestId, orderId, userId, skuId, null, requestId, OrderSagaStage.STARTED,
                OrderSagaStatus.RUNNING, attempts, version + 1, null, now, createdAt, now);
    }
}
