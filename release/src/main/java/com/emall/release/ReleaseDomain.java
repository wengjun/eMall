package com.emall.release;

import java.time.Instant;
import java.math.BigDecimal;

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

record FeatureToggle(long toggleId, String flagKey, String serviceName, ToggleStatus status, int rolloutPercent,
        Instant createdAt, Instant updatedAt) {
    FeatureToggle change(ToggleStatus nextStatus, int nextPercent) {
        return new FeatureToggle(toggleId, flagKey, serviceName, nextStatus, nextPercent, createdAt, Instant.now());
    }
}

record RolloutPlan(long rolloutId, String serviceName, String version, String strategy, int currentPercent,
        RolloutStatus status, Instant createdAt, Instant updatedAt) {
    RolloutPlan change(RolloutStatus nextStatus, int percent) {
        return new RolloutPlan(rolloutId, serviceName, version, strategy, percent, nextStatus, createdAt,
                Instant.now());
    }
}

record MessageTopicGovernance(long topicId, String topicName, String owner, String schemaVersion, long lagBudget,
        TopicStatus status, Instant createdAt, Instant updatedAt) {
    MessageTopicGovernance changeStatus(TopicStatus nextStatus) {
        return new MessageTopicGovernance(topicId, topicName, owner, schemaVersion, lagBudget, nextStatus, createdAt,
                Instant.now());
    }
}

record ReplayPlan(long replayId, String topicName, String consumerGroup, long fromOffset, long toOffset,
        RolloutStatus status, Instant createdAt, Instant updatedAt) {
    ReplayPlan changeStatus(RolloutStatus nextStatus) {
        return new ReplayPlan(replayId, topicName, consumerGroup, fromOffset, toOffset, nextStatus, createdAt,
                Instant.now());
    }
}

record ReleaseGuardRecord(long guardId, long rolloutId, String serviceName, ReleaseGuardStage stage,
        ReleaseGuardDecision decision, Boolean sloPassed, Boolean alertsClear, Boolean capacityReady,
        Boolean dependenciesHealthy, BigDecimal errorRate, Integer latencyP95Ms, BigDecimal businessSuccessRate,
        Boolean compensationTriggered, Boolean messageReplayChecked, Boolean downstreamRecovered, String reason,
        Instant createdAt) {
}

record ReleaseSummary(int enabledFlags, int runningRollouts, int activeTopics, int openReplays) {
}

record ReleaseGuardSummary(int passedGuards, int blockedGuards, int pausedGuards, int rollbackGuards) {
}
