package com.emall.platformops;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusPlatformOpsRepository implements PlatformOpsRepository {
    private final PlatformOpsMapper platformOpsMapper;

    MybatisPlusPlatformOpsRepository(PlatformOpsMapper platformOpsMapper) {
        this.platformOpsMapper = platformOpsMapper;
    }

    @Override
    public BackupPlan saveBackupPlan(BackupPlan plan) {
        platformOpsMapper.saveBackupPlan(plan);
        return plan;
    }

    @Override
    public Optional<BackupPlan> findBackupPlan(long planId) {
        return Optional.ofNullable(platformOpsMapper.findBackupPlan(planId));
    }

    @Override
    public List<BackupPlan> findBackupPlans() {
        return platformOpsMapper.findBackupPlans();
    }

    @Override
    public DatabaseOperation saveDatabaseOperation(DatabaseOperation operation) {
        platformOpsMapper.saveDatabaseOperation(operation);
        return operation;
    }

    @Override
    public Optional<DatabaseOperation> findDatabaseOperation(long operationId) {
        return Optional.ofNullable(platformOpsMapper.findDatabaseOperation(operationId));
    }

    @Override
    public List<DatabaseOperation> findDatabaseOperations() {
        return platformOpsMapper.findDatabaseOperations();
    }

    @Override
    public FinOpsAction saveFinOpsAction(FinOpsAction action) {
        platformOpsMapper.saveFinOpsAction(action);
        return action;
    }

    @Override
    public Optional<FinOpsAction> findFinOpsAction(long actionId) {
        return Optional.ofNullable(platformOpsMapper.findFinOpsAction(actionId));
    }

    @Override
    public List<FinOpsAction> findFinOpsActions() {
        return platformOpsMapper.findFinOpsActions();
    }

    @Override
    public SecurityOperation saveSecurityOperation(SecurityOperation operation) {
        platformOpsMapper.saveSecurityOperation(operation);
        return operation;
    }

    @Override
    public Optional<SecurityOperation> findSecurityOperation(long operationId) {
        return Optional.ofNullable(platformOpsMapper.findSecurityOperation(operationId));
    }

    @Override
    public List<SecurityOperation> findSecurityOperations() {
        return platformOpsMapper.findSecurityOperations();
    }
}
