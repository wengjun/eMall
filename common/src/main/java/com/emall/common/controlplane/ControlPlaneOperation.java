package com.emall.common.controlplane;

import java.time.Instant;
import java.util.Map;

public record ControlPlaneOperation(String operationId, String idempotencyKey, String module, ControlPlaneTarget target,
        String action, String resourceType, String resourceId, Map<String, Object> desiredState, String desiredDigest,
        Map<String, Object> rollbackState, Map<String, Object> observedState, ControlPlaneOperationStatus status,
        int attemptCount, int maxAttempts, Instant nextAttemptAt, String leaseOwner, Instant leaseUntil,
        String lastError, Instant createdAt, Instant updatedAt) {
    public boolean terminal() {
        return status.terminal();
    }
}
