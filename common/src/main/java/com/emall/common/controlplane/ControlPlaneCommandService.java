package com.emall.common.controlplane;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ControlPlaneCommandService implements ControlPlaneClient {
    private final ControlPlaneOperationStore store;
    private final ControlPlaneProperties properties;
    private final ControlPlaneJson json;
    private final Clock clock;

    public ControlPlaneCommandService(ControlPlaneOperationStore store, ControlPlaneProperties properties,
            ObjectMapper objectMapper, Clock clock) {
        this.store = store;
        this.properties = properties;
        this.json = new ControlPlaneJson(objectMapper);
        this.clock = clock;
    }

    @Override
    @Transactional
    public ControlPlaneOperation submit(ControlPlaneCommand command) {
        Instant now = clock.instant();
        String digest = json.digest(command.desiredState());
        ControlPlaneOperation proposed = new ControlPlaneOperation(UUID.randomUUID().toString(),
                command.idempotencyKey(), command.module(), command.target(), command.action(), command.resourceType(),
                command.resourceId(), command.desiredState(), digest, null, null, ControlPlaneOperationStatus.PENDING,
                0, properties.getMaxAttempts(), now, null, null, null, now, now);
        ControlPlaneOperation operation = store.insertIfAbsent(proposed);
        if (!operation.desiredDigest().equals(digest)) {
            throw new IllegalStateException("idempotency key was already used with a different desired state");
        }
        return operation;
    }

    @Override
    public Optional<ControlPlaneOperation> find(String operationId) {
        return store.find(operationId);
    }

    @Override
    public Optional<ControlPlaneOperation> findByIdempotencyKey(String idempotencyKey) {
        return store.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public Optional<ControlPlaneOperation> findLatest(String module, String resourceType, String resourceId) {
        return store.findLatest(module, resourceType, resourceId);
    }
}
