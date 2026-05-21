package com.emall.experiment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

@TableName("experiment_definition")
record ExperimentDefinition(@TableId(value = "experiment_id", type = IdType.INPUT) long experimentId, String scene,
        String name, String mutualExclusionGroup, int trafficPercent, String controlVariant, String treatmentVariant,
        ExperimentStatus status, Instant createdAt, Instant updatedAt) {
    ExperimentDefinition changeStatus(ExperimentStatus nextStatus) {
        return new ExperimentDefinition(experimentId, scene, name, mutualExclusionGroup, trafficPercent, controlVariant,
                treatmentVariant, nextStatus, createdAt, Instant.now());
    }
}

@TableName("experiment_guardrail")
record GuardrailMetric(@TableId(value = "metric_id", type = IdType.INPUT) long metricId, long experimentId,
        String metricName, GuardrailDirection direction, @TableField("threshold_value") BigDecimal threshold,
        Instant createdAt) {
}

@TableName("experiment_metric")
record ExperimentMetric(@TableId(value = "metric_record_id", type = IdType.INPUT) long metricRecordId,
        long experimentId, String variant, String metricName, @TableField("metric_value") BigDecimal value,
        Instant recordedAt) {
}

record ExperimentAssignment(long experimentId, String scene, String userKey, String variant, String bucket) {
}

record ExperimentReport(long experimentId, BigDecimal controlMetric, BigDecimal treatmentMetric,
        boolean guardrailBreached, boolean rollbackRecommended) {
}
