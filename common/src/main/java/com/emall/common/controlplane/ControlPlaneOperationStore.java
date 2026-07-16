package com.emall.common.controlplane;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ControlPlaneOperationStore {
    ControlPlaneOperation insertIfAbsent(ControlPlaneOperation operation);

    Optional<ControlPlaneOperation> find(String operationId);

    Optional<ControlPlaneOperation> findByIdempotencyKey(String idempotencyKey);

    Optional<ControlPlaneOperation> findLatest(String module, String resourceType, String resourceId);

    List<ControlPlaneOperation> findClaimable(Instant now, int limit);

    boolean claim(String operationId, String owner, Instant leaseUntil, Instant now);

    boolean saveRollbackState(String operationId, String owner, Map<String, Object> rollbackState, Instant now);

    boolean transition(String operationId, String owner, ControlPlaneOperationStatus status, int attemptCount,
            Map<String, Object> observedState, String lastError, Instant nextAttemptAt, boolean releaseLease,
            Instant now);
}
