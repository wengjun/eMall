package com.emall.platformops;

import com.emall.common.controlplane.ControlPlaneClient;
import com.emall.common.controlplane.ControlPlaneCommands;
import com.emall.common.controlplane.ControlPlaneIdempotencyKeys;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class PlatformControlPlanePublisher {
    private static final String MODULE = "platform-ops";

    private final ControlPlaneClient client;

    PlatformControlPlanePublisher(ControlPlaneClient client) {
        this.client = client;
    }

    void reconcileBackupPlan(BackupPlan plan) {
        Map<String, Object> desired = new LinkedHashMap<>();
        desired.put("planId", plan.planId());
        desired.put("databaseName", plan.databaseName());
        desired.put("backupType", plan.backupType());
        desired.put("retentionDays", plan.retentionDays());
        desired.put("desiredStatus", plan.status().name());
        submit("reconcile-backup-plan", "database-backup", plan.planId(), desired);
    }

    void executeDatabaseOperation(DatabaseOperation operation) {
        Map<String, Object> desired = new LinkedHashMap<>();
        desired.put("operationId", operation.operationId());
        desired.put("databaseName", operation.databaseName());
        desired.put("operationType", operation.operationType());
        desired.put("riskLevel", operation.riskLevel().name());
        desired.put("detail", operation.detail());
        desired.put("desiredStatus", operation.status().name());
        submit("execute-database-operation", "database-operation", operation.operationId(), desired);
    }

    void executeFinOpsAction(FinOpsAction action) {
        Map<String, Object> desired = new LinkedHashMap<>();
        desired.put("actionId", action.actionId());
        desired.put("serviceName", action.serviceName());
        desired.put("actionType", action.actionType());
        desired.put("estimatedSaving", action.estimatedSaving());
        desired.put("desiredStatus", action.status().name());
        submit("execute-finops-action", "finops-action", action.actionId(), desired);
    }

    void executeSecurityOperation(SecurityOperation operation) {
        Map<String, Object> desired = new LinkedHashMap<>();
        desired.put("operationId", operation.operationId());
        desired.put("serviceName", operation.serviceName());
        desired.put("signalType", operation.signalType());
        desired.put("riskLevel", operation.riskLevel().name());
        desired.put("desiredStatus", operation.status().name());
        submit("execute-security-operation", "security-operation", operation.operationId(), desired);
    }

    private void submit(String action, String resourceType, long resourceId, Map<String, Object> desired) {
        String id = Long.toString(resourceId);
        String suffix = id + '-' + desired.get("desiredStatus");
        String key = ControlPlaneIdempotencyKeys.currentOrDeterministic(MODULE, action, suffix);
        client.submit(ControlPlaneCommands.infrastructure(key, MODULE, action, resourceType, id, desired));
    }
}
