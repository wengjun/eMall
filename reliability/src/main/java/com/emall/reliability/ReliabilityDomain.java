package com.emall.reliability;

import java.math.BigDecimal;
import java.time.Instant;

enum GateStatus {
    OPEN,
    PASSED,
    BLOCKED
}

record CapacityRehearsal(long rehearsalId, String serviceName, int targetQps, int peakConcurrency,
                         GateStatus status, Instant createdAt, Instant updatedAt) {
    CapacityRehearsal changeStatus(GateStatus nextStatus) {
        return new CapacityRehearsal(rehearsalId, serviceName, targetQps, peakConcurrency, nextStatus, createdAt,
                Instant.now());
    }
}

record SloObjective(long sloId, String serviceName, BigDecimal availabilityTarget, int latencyP95Ms,
                    BigDecimal errorBudgetPercent, Instant createdAt) {
}

record ChaosSchedule(long chaosId, String serviceName, String drillType, int blastRadiusPercent,
                     GateStatus approvalStatus, Instant scheduledAt, Instant createdAt) {
    ChaosSchedule approve() {
        return new ChaosSchedule(chaosId, serviceName, drillType, blastRadiusPercent, GateStatus.PASSED,
                scheduledAt, createdAt);
    }
}

record ReadinessGate(long gateId, String serviceName, boolean runbookReady, boolean dashboardReady,
                     boolean rollbackReady, GateStatus status, Instant createdAt, Instant updatedAt) {
}

record ReliabilitySummary(int rehearsals, int approvedChaos, int blockedReadinessGates, int sloObjectives) {
}
