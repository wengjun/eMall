package com.emall.traffic;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

enum UnitStatus {
    ACTIVE,
    DRAINING,
    ISOLATED
}

enum ShiftStatus {
    PLANNED,
    RUNNING,
    COMPLETED,
    ABORTED
}

enum ControlRuleType {
    RATE_LIMIT,
    CIRCUIT_BREAKER,
    DEGRADE
}

@TableName("unit_cell")
record UnitCell(@TableId(value = "unit_id", type = IdType.INPUT) long unitId, String unitCode, String regionCode,
        int capacityWeight, UnitStatus status, Instant createdAt, Instant updatedAt) {
    UnitCell changeStatus(UnitStatus nextStatus) {
        return new UnitCell(unitId, unitCode, regionCode, capacityWeight, nextStatus, createdAt, Instant.now());
    }
}

@TableName("shard_route")
record ShardRoute(@TableId(value = "route_id", type = IdType.INPUT) long routeId, String domainName, int shardNo,
        String unitCode, String databaseKey, Instant updatedAt) {
}

@TableName("traffic_shift")
record TrafficShift(@TableId(value = "shift_id", type = IdType.INPUT) long shiftId, String sourceUnit,
        String targetUnit, int percent, ShiftStatus status, String reason, Instant createdAt, Instant updatedAt) {
    TrafficShift changeStatus(ShiftStatus nextStatus) {
        return new TrafficShift(shiftId, sourceUnit, targetUnit, percent, nextStatus, reason, createdAt, Instant.now());
    }
}

@TableName("traffic_control_rule")
record TrafficControlRule(@TableId(value = "rule_id", type = IdType.INPUT) long ruleId, String resource,
        ControlRuleType type, String dimension, String matchValue, @TableField("threshold_value") int threshold,
        String unitCode, boolean enabled, Instant createdAt, Instant updatedAt) {
    TrafficControlRule changeEnabled(boolean nextEnabled) {
        return new TrafficControlRule(ruleId, resource, type, dimension, matchValue, threshold, unitCode, nextEnabled,
                createdAt, Instant.now());
    }
}

record TrafficSummary(int activeUnits, int shardRoutes, int runningShifts, int isolatedUnits, int controlRules) {
}
