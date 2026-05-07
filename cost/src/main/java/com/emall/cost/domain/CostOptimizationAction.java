package com.emall.cost.domain;

import java.time.Instant;

public record CostOptimizationAction(long actionId, String serviceName, CostSignalType signalType,
        CostActionType actionType, CostActionStatus status, int priority, String description, Instant createdAt,
        Instant updatedAt) {
    public CostOptimizationAction changeStatus(CostActionStatus nextStatus) {
        return new CostOptimizationAction(actionId, serviceName, signalType, actionType, nextStatus, priority,
                description, createdAt, Instant.now());
    }
}
