package com.emall.common.controlplane;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryControlPlaneOperationStore implements ControlPlaneOperationStore {
    private final ConcurrentMap<String, ControlPlaneOperation> operations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> idempotencyIndex = new ConcurrentHashMap<>();

    @Override
    public ControlPlaneOperation insertIfAbsent(ControlPlaneOperation operation) {
        String operationId =
                idempotencyIndex.computeIfAbsent(operation.idempotencyKey(), key -> operation.operationId());
        operations.putIfAbsent(operationId, operation);
        return operations.get(operationId);
    }

    @Override
    public Optional<ControlPlaneOperation> find(String operationId) {
        return Optional.ofNullable(operations.get(operationId));
    }

    @Override
    public Optional<ControlPlaneOperation> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(idempotencyIndex.get(idempotencyKey)).flatMap(this::find);
    }

    @Override
    public Optional<ControlPlaneOperation> findLatest(String module, String resourceType, String resourceId) {
        return operations.values().stream()
                .filter(operation -> operation.module().equals(module) && operation.resourceType().equals(resourceType)
                        && operation.resourceId().equals(resourceId))
                .max(Comparator.comparing(ControlPlaneOperation::createdAt));
    }

    @Override
    public List<ControlPlaneOperation> findClaimable(Instant now, int limit) {
        return operations.values().stream().filter(operation -> !operation.terminal())
                .filter(operation -> !operation.nextAttemptAt().isAfter(now))
                .filter(operation -> operation.leaseUntil() == null || !operation.leaseUntil().isAfter(now))
                .sorted(Comparator.comparing(ControlPlaneOperation::nextAttemptAt)
                        .thenComparing(ControlPlaneOperation::createdAt))
                .limit(Math.max(1, limit)).toList();
    }

    @Override
    public boolean claim(String operationId, String owner, Instant leaseUntil, Instant now) {
        final boolean[] claimed = {false};
        operations.computeIfPresent(operationId, (key, operation) -> {
            if (operation.terminal() || operation.leaseUntil() != null && operation.leaseUntil().isAfter(now)) {
                return operation;
            }
            claimed[0] = true;
            return copy(operation, operation.rollbackState(), operation.observedState(), operation.status(),
                    operation.attemptCount(), operation.nextAttemptAt(), owner, leaseUntil, operation.lastError(), now);
        });
        return claimed[0];
    }

    @Override
    public boolean saveRollbackState(String operationId, String owner, Map<String, Object> rollbackState, Instant now) {
        final boolean[] saved = {false};
        operations.computeIfPresent(operationId, (key, operation) -> {
            if (!owner.equals(operation.leaseOwner()) || operation.rollbackState() != null) {
                return operation;
            }
            saved[0] = true;
            return copy(operation, Map.copyOf(rollbackState), operation.observedState(), operation.status(),
                    operation.attemptCount(), operation.nextAttemptAt(), operation.leaseOwner(), operation.leaseUntil(),
                    operation.lastError(), now);
        });
        return saved[0];
    }

    @Override
    public boolean transition(String operationId, String owner, ControlPlaneOperationStatus status, int attemptCount,
            Map<String, Object> observedState, String lastError, Instant nextAttemptAt, boolean releaseLease,
            Instant now) {
        final boolean[] updated = {false};
        operations.computeIfPresent(operationId, (key, operation) -> {
            if (!owner.equals(operation.leaseOwner())) {
                return operation;
            }
            updated[0] = true;
            return copy(operation, operation.rollbackState(), observedState, status, attemptCount, nextAttemptAt,
                    releaseLease ? null : owner, releaseLease ? null : operation.leaseUntil(), lastError, now);
        });
        return updated[0];
    }

    private ControlPlaneOperation copy(ControlPlaneOperation operation, Map<String, Object> rollbackState,
            Map<String, Object> observedState, ControlPlaneOperationStatus status, int attemptCount,
            Instant nextAttemptAt, String leaseOwner, Instant leaseUntil, String lastError, Instant updatedAt) {
        return new ControlPlaneOperation(operation.operationId(), operation.idempotencyKey(), operation.module(),
                operation.target(), operation.action(), operation.resourceType(), operation.resourceId(),
                operation.desiredState(), operation.desiredDigest(), rollbackState, observedState, status, attemptCount,
                operation.maxAttempts(), nextAttemptAt, leaseOwner, leaseUntil, lastError, operation.createdAt(),
                updatedAt);
    }
}
