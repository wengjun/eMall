package com.emall.platformops;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryPlatformOpsRepository implements PlatformOpsRepository {
    private final ConcurrentMap<Long, BackupPlan> backupPlans = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, DatabaseOperation> databaseOperations = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, FinOpsAction> finOpsActions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, SecurityOperation> securityOperations = new ConcurrentHashMap<>();

    @Override
    public BackupPlan saveBackupPlan(BackupPlan plan) {
        backupPlans.put(plan.planId(), plan);
        return plan;
    }

    @Override
    public Optional<BackupPlan> findBackupPlan(long planId) {
        return Optional.ofNullable(backupPlans.get(planId));
    }

    @Override
    public List<BackupPlan> findBackupPlans() {
        return List.copyOf(backupPlans.values());
    }

    @Override
    public DatabaseOperation saveDatabaseOperation(DatabaseOperation operation) {
        databaseOperations.put(operation.operationId(), operation);
        return operation;
    }

    @Override
    public Optional<DatabaseOperation> findDatabaseOperation(long operationId) {
        return Optional.ofNullable(databaseOperations.get(operationId));
    }

    @Override
    public List<DatabaseOperation> findDatabaseOperations() {
        return List.copyOf(databaseOperations.values());
    }

    @Override
    public FinOpsAction saveFinOpsAction(FinOpsAction action) {
        finOpsActions.put(action.actionId(), action);
        return action;
    }

    @Override
    public Optional<FinOpsAction> findFinOpsAction(long actionId) {
        return Optional.ofNullable(finOpsActions.get(actionId));
    }

    @Override
    public List<FinOpsAction> findFinOpsActions() {
        return List.copyOf(finOpsActions.values());
    }

    @Override
    public SecurityOperation saveSecurityOperation(SecurityOperation operation) {
        securityOperations.put(operation.operationId(), operation);
        return operation;
    }

    @Override
    public Optional<SecurityOperation> findSecurityOperation(long operationId) {
        return Optional.ofNullable(securityOperations.get(operationId));
    }

    @Override
    public List<SecurityOperation> findSecurityOperations() {
        return List.copyOf(securityOperations.values());
    }
}
