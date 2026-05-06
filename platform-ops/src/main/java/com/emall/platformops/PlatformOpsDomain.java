package com.emall.platformops;

import java.math.BigDecimal;
import java.time.Instant;

enum OpsStatus {
    OPEN,
    APPROVED,
    RUNNING,
    COMPLETED,
    BLOCKED
}

enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

record BackupPlan(long planId, String databaseName, String backupType, int retentionDays, OpsStatus status,
                  Instant createdAt, Instant updatedAt) {
    BackupPlan changeStatus(OpsStatus nextStatus) {
        return new BackupPlan(planId, databaseName, backupType, retentionDays, nextStatus, createdAt, Instant.now());
    }
}

record DatabaseOperation(long operationId, String databaseName, String operationType, RiskLevel riskLevel,
                         OpsStatus status, String detail, Instant createdAt, Instant updatedAt) {
    DatabaseOperation changeStatus(OpsStatus nextStatus) {
        return new DatabaseOperation(operationId, databaseName, operationType, riskLevel, nextStatus, detail,
                createdAt, Instant.now());
    }
}

record FinOpsAction(long actionId, String serviceName, String actionType, BigDecimal estimatedSaving,
                    OpsStatus status, Instant createdAt, Instant updatedAt) {
    FinOpsAction changeStatus(OpsStatus nextStatus) {
        return new FinOpsAction(actionId, serviceName, actionType, estimatedSaving, nextStatus, createdAt,
                Instant.now());
    }
}

record SecurityOperation(long operationId, String serviceName, String signalType, RiskLevel riskLevel,
                         OpsStatus status, Instant createdAt, Instant updatedAt) {
    SecurityOperation changeStatus(OpsStatus nextStatus) {
        return new SecurityOperation(operationId, serviceName, signalType, riskLevel, nextStatus, createdAt,
                Instant.now());
    }
}

record PlatformOpsSummary(int backupPlans, int blockedDatabaseOps, int approvedFinOpsActions,
                          int criticalSecuritySignals) {
}
