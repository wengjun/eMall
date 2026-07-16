package com.emall.common.controlplane;

import java.util.Optional;

public interface ControlPlaneClient {
    ControlPlaneOperation submit(ControlPlaneCommand command);

    Optional<ControlPlaneOperation> find(String operationId);

    Optional<ControlPlaneOperation> findByIdempotencyKey(String idempotencyKey);

    Optional<ControlPlaneOperation> findLatest(String module, String resourceType, String resourceId);
}
