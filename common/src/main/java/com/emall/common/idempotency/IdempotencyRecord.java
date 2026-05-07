package com.emall.common.idempotency;

import java.time.Instant;

public record IdempotencyRecord(String requestId, String businessType, String businessId, String status,
        Instant createdAt, Instant updatedAt) {
    public static IdempotencyRecord completed(String requestId, String businessType, String businessId) {
        Instant now = Instant.now();
        return new IdempotencyRecord(requestId, businessType, businessId, "COMPLETED", now, now);
    }
}
