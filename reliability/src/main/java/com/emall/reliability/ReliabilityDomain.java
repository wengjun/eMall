package com.emall.reliability;

import java.math.BigDecimal;
import java.time.Instant;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

enum GateStatus {
    OPEN,
    PASSED,
    BLOCKED
}

@TableName("capacity_rehearsal")
record CapacityRehearsal(@TableId(value = "rehearsal_id", type = IdType.INPUT) long rehearsalId, String serviceName,
        int targetQps, int peakConcurrency, GateStatus status, Instant createdAt, Instant updatedAt) {
    CapacityRehearsal changeStatus(GateStatus nextStatus) {
        return new CapacityRehearsal(rehearsalId, serviceName, targetQps, peakConcurrency, nextStatus, createdAt,
                Instant.now());
    }
}

@TableName("slo_objective")
record SloObjective(@TableId(value = "slo_id", type = IdType.INPUT) long sloId, String serviceName,
        BigDecimal availabilityTarget,
        @TableField("latency_p95_ms") int latencyP95Ms,
        BigDecimal errorBudgetPercent, Instant createdAt) {
}

@TableName("chaos_schedule")
record ChaosSchedule(@TableId(value = "chaos_id", type = IdType.INPUT) long chaosId, String serviceName,
        String drillType, int blastRadiusPercent, GateStatus approvalStatus, Instant scheduledAt, Instant createdAt) {
    ChaosSchedule approve() {
        return new ChaosSchedule(chaosId, serviceName, drillType, blastRadiusPercent, GateStatus.PASSED, scheduledAt,
                createdAt);
    }
}

@TableName("readiness_gate")
record ReadinessGate(@TableId(value = "gate_id", type = IdType.INPUT) long gateId, String serviceName,
        boolean runbookReady, boolean dashboardReady,
        boolean rollbackReady, GateStatus status, Instant createdAt, Instant updatedAt) {
}

record ReliabilitySummary(int rehearsals, int approvedChaos, int blockedReadinessGates, int sloObjectives) {
}
