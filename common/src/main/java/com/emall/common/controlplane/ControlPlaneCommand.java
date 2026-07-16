package com.emall.common.controlplane;

import java.util.Map;
import java.util.Objects;

public record ControlPlaneCommand(String idempotencyKey, String module, ControlPlaneTarget target, String action,
        String resourceType, String resourceId, Map<String, Object> desiredState) {
    public ControlPlaneCommand {
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        module = requireText(module, "module");
        target = Objects.requireNonNull(target, "target must not be null");
        action = requireText(action, "action");
        resourceType = requireText(resourceType, "resourceType");
        resourceId = requireText(resourceId, "resourceId");
        desiredState = Map.copyOf(Objects.requireNonNull(desiredState, "desiredState must not be null"));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
