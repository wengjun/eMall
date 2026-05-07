package com.emall.recommendation.domain;

import java.time.Instant;

public record Experiment(long experimentId, String scene, String name, int trafficPercent, String controlStrategy,
        String treatmentStrategy, ExperimentStatus status, Instant createdAt, Instant updatedAt) {
    public Experiment changeStatus(ExperimentStatus newStatus) {
        return new Experiment(experimentId, scene, name, trafficPercent, controlStrategy, treatmentStrategy, newStatus,
                createdAt, Instant.now());
    }
}
