package com.emall.common.idempotency;

import java.util.Locale;
import java.util.Objects;

public record IdempotencyKey(String namespace, String ownerId, String requestId, String operation) {
    private static final int MAX_SEGMENT_LENGTH = 128;

    public IdempotencyKey {
        namespace = normalizeRequired(namespace, "namespace").toLowerCase(Locale.ROOT);
        ownerId = normalizeRequired(ownerId, "ownerId");
        requestId = normalizeRequired(requestId, "requestId");
        operation = normalizeRequired(operation, "operation").toLowerCase(Locale.ROOT);
    }

    public static IdempotencyKey of(String namespace, String ownerId, String requestId, String operation) {
        return new IdempotencyKey(namespace, ownerId, requestId, operation);
    }

    public String storageKey() {
        return namespace + ":" + ownerId + ":" + operation + ":" + requestId;
    }

    private static String normalizeRequired(String value, String name) {
        String normalized = Objects.requireNonNull(value, name + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (normalized.length() > MAX_SEGMENT_LENGTH) {
            throw new IllegalArgumentException(name + " must not exceed " + MAX_SEGMENT_LENGTH + " characters");
        }
        return normalized;
    }
}
