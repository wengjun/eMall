package com.emall.common.controlplane;

import java.util.Map;

public interface ControlPlaneAdapter {
    ControlPlaneTarget target();

    Map<String, Object> captureRollbackState(ControlPlaneOperation operation);

    void apply(ControlPlaneOperation operation);

    ControlPlaneObservation observe(ControlPlaneOperation operation);

    void rollback(ControlPlaneOperation operation);

    ControlPlaneObservation observeRollback(ControlPlaneOperation operation);
}
