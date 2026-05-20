package com.emall.common.idempotency;

import java.time.Instant;

public record IdempotencyRecord(String key, String requestId, String businessType, String businessId, String operation,
        String ownerId, String requestDigest, String responseDigest, IdempotencyStatus status, Instant lockedUntil,
        Instant expiresAt, Instant createdAt, Instant updatedAt) {
    public static IdempotencyRecord processing(IdempotencyKey key, String businessType, String businessId,
            String requestDigest, Instant lockedUntil, Instant expiresAt) {
        Instant now = Instant.now();
        return new IdempotencyRecord(key.storageKey(), key.requestId(), businessType, businessId, key.operation(),
                key.ownerId(), requestDigest, null, IdempotencyStatus.PROCESSING, lockedUntil, expiresAt, now, now);
    }

    public static IdempotencyRecord completed(String requestId, String businessType, String businessId) {
        Instant now = Instant.now();
        IdempotencyKey key = IdempotencyKey.of(businessType, businessId, requestId, "legacy");
        return new IdempotencyRecord(key.storageKey(), requestId, businessType, businessId, key.operation(),
                key.ownerId(), null, null, IdempotencyStatus.SUCCEEDED, null, null, now, now);
    }

    public IdempotencyRecord processingAgain(Instant lockedUntil, Instant now) {
        return new IdempotencyRecord(key, requestId, businessType, businessId, operation, ownerId, requestDigest,
                responseDigest, IdempotencyStatus.PROCESSING, lockedUntil, expiresAt, createdAt, now);
    }

    public IdempotencyRecord succeeded(String responseDigest, Instant now) {
        return new IdempotencyRecord(key, requestId, businessType, businessId, operation, ownerId, requestDigest,
                responseDigest, IdempotencyStatus.SUCCEEDED, null, expiresAt, createdAt, now);
    }

    public IdempotencyRecord retryableFailed(String responseDigest, Instant now) {
        return new IdempotencyRecord(key, requestId, businessType, businessId, operation, ownerId, requestDigest,
                responseDigest, IdempotencyStatus.RETRYABLE_FAILED, null, expiresAt, createdAt, now);
    }

    public IdempotencyRecord terminalFailed(String responseDigest, Instant now) {
        return new IdempotencyRecord(key, requestId, businessType, businessId, operation, ownerId, requestDigest,
                responseDigest, IdempotencyStatus.TERMINAL_FAILED, null, expiresAt, createdAt, now);
    }

    public boolean requestMatches(String digest) {
        return requestDigest == null || requestDigest.equals(digest);
    }

    public boolean processingLockActive(Instant now) {
        return status == IdempotencyStatus.PROCESSING && lockedUntil != null && lockedUntil.isAfter(now);
    }
}
