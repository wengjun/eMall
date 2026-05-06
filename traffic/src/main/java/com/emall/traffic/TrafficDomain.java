package com.emall.traffic;

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

record UnitCell(long unitId, String unitCode, String regionCode, int capacityWeight, UnitStatus status,
                Instant createdAt, Instant updatedAt) {
    UnitCell changeStatus(UnitStatus nextStatus) {
        return new UnitCell(unitId, unitCode, regionCode, capacityWeight, nextStatus, createdAt, Instant.now());
    }
}

record ShardRoute(long routeId, String domainName, int shardNo, String unitCode, String databaseKey,
                  Instant updatedAt) {
}

record TrafficShift(long shiftId, String sourceUnit, String targetUnit, int percent, ShiftStatus status,
                    String reason, Instant createdAt, Instant updatedAt) {
    TrafficShift changeStatus(ShiftStatus nextStatus) {
        return new TrafficShift(shiftId, sourceUnit, targetUnit, percent, nextStatus, reason, createdAt,
                Instant.now());
    }
}

record TrafficSummary(int activeUnits, int shardRoutes, int runningShifts, int isolatedUnits) {
}
