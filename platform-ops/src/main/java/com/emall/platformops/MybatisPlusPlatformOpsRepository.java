package com.emall.platformops;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusPlatformOpsRepository implements PlatformOpsRepository {
    private final PlatformOpsMapper platformOpsMapper;
    private final BackupPlanMapper backupPlanMapper;
    private final DatabaseOperationMapper databaseOperationMapper;
    private final FinOpsActionMapper finOpsActionMapper;
    private final SecurityOperationMapper securityOperationMapper;

    MybatisPlusPlatformOpsRepository(PlatformOpsMapper platformOpsMapper, BackupPlanMapper backupPlanMapper,
            DatabaseOperationMapper databaseOperationMapper, FinOpsActionMapper finOpsActionMapper,
            SecurityOperationMapper securityOperationMapper) {
        this.platformOpsMapper = platformOpsMapper;
        this.backupPlanMapper = backupPlanMapper;
        this.databaseOperationMapper = databaseOperationMapper;
        this.finOpsActionMapper = finOpsActionMapper;
        this.securityOperationMapper = securityOperationMapper;
    }

    @Override
    public BackupPlan saveBackupPlan(BackupPlan plan) {
        platformOpsMapper.saveBackupPlan(plan);
        return plan;
    }

    @Override
    public Optional<BackupPlan> findBackupPlan(long planId) {
        return Optional.ofNullable(backupPlanMapper.selectById(planId));
    }

    @Override
    public List<BackupPlan> findBackupPlans() {
        return backupPlanMapper.selectList(null);
    }

    @Override
    public DatabaseOperation saveDatabaseOperation(DatabaseOperation operation) {
        platformOpsMapper.saveDatabaseOperation(operation);
        return operation;
    }

    @Override
    public Optional<DatabaseOperation> findDatabaseOperation(long operationId) {
        return Optional.ofNullable(databaseOperationMapper.selectById(operationId));
    }

    @Override
    public List<DatabaseOperation> findDatabaseOperations() {
        return databaseOperationMapper.selectList(null);
    }

    @Override
    public FinOpsAction saveFinOpsAction(FinOpsAction action) {
        platformOpsMapper.saveFinOpsAction(action);
        return action;
    }

    @Override
    public Optional<FinOpsAction> findFinOpsAction(long actionId) {
        return Optional.ofNullable(finOpsActionMapper.selectById(actionId));
    }

    @Override
    public List<FinOpsAction> findFinOpsActions() {
        return finOpsActionMapper.selectList(null);
    }

    @Override
    public SecurityOperation saveSecurityOperation(SecurityOperation operation) {
        platformOpsMapper.saveSecurityOperation(operation);
        return operation;
    }

    @Override
    public Optional<SecurityOperation> findSecurityOperation(long operationId) {
        return Optional.ofNullable(securityOperationMapper.selectById(operationId));
    }

    @Override
    public List<SecurityOperation> findSecurityOperations() {
        return securityOperationMapper.selectList(null);
    }
}
