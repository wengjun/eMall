package com.emall.release;

import java.time.Instant;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

enum RolloutStatus {
    PLANNED,
    RUNNING,
    PAUSED,
    COMPLETED,
    ROLLED_BACK
}

enum ReleaseGuardStage {
    PRE_TRAFFIC,
    CANARY,
    ROLLBACK_RECOVERY
}

enum ReleaseGuardDecision {
    PASS,
    BLOCK,
    PAUSE,
    ROLLBACK
}

enum ToggleStatus {
    ON,
    OFF
}

enum TopicStatus {
    ACTIVE,
    DRAINING,
    DEPRECATED
}

@TableName("feature_toggle")
record FeatureToggle(@TableId(value = "toggle_id", type = IdType.INPUT) long toggleId, String flagKey,
        String serviceName, ToggleStatus status, int rolloutPercent, Instant createdAt, Instant updatedAt) {
    FeatureToggle change(ToggleStatus nextStatus, int nextPercent) {
        return new FeatureToggle(toggleId, flagKey, serviceName, nextStatus, nextPercent, createdAt, Instant.now());
    }
}

@TableName("rollout_plan")
record RolloutPlan(@TableId(value = "rollout_id", type = IdType.INPUT) long rolloutId, String serviceName,
        String version, String strategy, int currentPercent, RolloutStatus status, Instant createdAt,
        Instant updatedAt) {
    RolloutPlan change(RolloutStatus nextStatus, int percent) {
        return new RolloutPlan(rolloutId, serviceName, version, strategy, percent, nextStatus, createdAt,
                Instant.now());
    }
}

@TableName("message_topic_governance")
record MessageTopicGovernance(@TableId(value = "topic_id", type = IdType.INPUT) long topicId, String topicName,
        String owner, String schemaVersion, long lagBudget, TopicStatus status, Instant createdAt, Instant updatedAt) {
    MessageTopicGovernance changeStatus(TopicStatus nextStatus) {
        return new MessageTopicGovernance(topicId, topicName, owner, schemaVersion, lagBudget, nextStatus, createdAt,
                Instant.now());
    }
}

@TableName("replay_plan")
record ReplayPlan(@TableId(value = "replay_id", type = IdType.INPUT) long replayId, String topicName,
        String consumerGroup, long fromOffset, long toOffset, RolloutStatus status, Instant createdAt,
        Instant updatedAt) {
    ReplayPlan changeStatus(RolloutStatus nextStatus) {
        return new ReplayPlan(replayId, topicName, consumerGroup, fromOffset, toOffset, nextStatus, createdAt,
                Instant.now());
    }
}

@TableName("release_guard_record")
record ReleaseGuardRecord(@TableId(value = "guard_id", type = IdType.INPUT) long guardId, long rolloutId,
        String serviceName, ReleaseGuardStage stage,
        ReleaseGuardDecision decision, Boolean sloPassed, Boolean alertsClear, Boolean capacityReady,
        Boolean dependenciesHealthy, BigDecimal errorRate, @TableField("latency_p95_ms") Integer latencyP95Ms,
        BigDecimal businessSuccessRate,
        Boolean compensationTriggered, Boolean messageReplayChecked, Boolean downstreamRecovered, String reason,
        Instant createdAt) {
}

record ReleaseSummary(int enabledFlags, int runningRollouts, int activeTopics, int openReplays) {
}

record ReleaseGuardSummary(int passedGuards, int blockedGuards, int pausedGuards, int rollbackGuards) {
}
