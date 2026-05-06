package com.emall.aftersales.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record AfterSalesRequest(
        long requestId,
        long orderId,
        long userId,
        long skuId,
        int quantity,
        BigDecimal refundAmount,
        AfterSalesType type,
        AfterSalesStatus status,
        String reason,
        Instant createdAt,
        Instant updatedAt
) {
    public AfterSalesRequest changeStatus(AfterSalesStatus newStatus) {
        return new AfterSalesRequest(requestId, orderId, userId, skuId, quantity, refundAmount,
                type, newStatus, reason, createdAt, Instant.now());
    }
}
