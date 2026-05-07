package com.emall.experiment;

import java.math.BigDecimal;
import java.time.Instant;

enum ExperimentStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    ROLLED_BACK,
    COMPLETED
}

enum GuardrailDirection {
    MAX,
    MIN
}

record ExperimentDefinition(long experimentId, String scene, String name, String mutualExclusionGroup,
        int trafficPercent, String controlVariant, String treatmentVariant, ExperimentStatus status, Instant createdAt,
        Instant updatedAt) {
    ExperimentDefinition changeStatus(ExperimentStatus nextStatus) {
        return new ExperimentDefinition(experimentId, scene, name, mutualExclusionGroup, trafficPercent, controlVariant,
                treatmentVariant, nextStatus, createdAt, Instant.now());
    }
}

record GuardrailMetric(long metricId, long experimentId, String metricName, GuardrailDirection direction,
        BigDecimal threshold, Instant createdAt) {
}

record ExperimentMetric(long metricRecordId, long experimentId, String variant, String metricName, BigDecimal value,
        Instant recordedAt) {
}

record ExperimentAssignment(long experimentId, String scene, String userKey, String variant, String bucket) {
}

record ExperimentReport(long experimentId, BigDecimal controlMetric, BigDecimal treatmentMetric,
        boolean guardrailBreached, boolean rollbackRecommended) {
}
