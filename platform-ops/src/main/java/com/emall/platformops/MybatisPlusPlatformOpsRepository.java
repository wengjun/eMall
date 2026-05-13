package com.emall.platformops;

import static com.emall.common.persistence.RowMaps.decimalValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
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
        return Optional.ofNullable(platformOpsMapper.findBackupPlan(planId)).map(this::mapBackupPlan);
    }

    @Override
    public List<BackupPlan> findBackupPlans() {
        return platformOpsMapper.findBackupPlans().stream().map(this::mapBackupPlan).toList();
    }

    @Override
    public DatabaseOperation saveDatabaseOperation(DatabaseOperation operation) {
        platformOpsMapper.saveDatabaseOperation(operation);
        return operation;
    }

    @Override
    public Optional<DatabaseOperation> findDatabaseOperation(long operationId) {
        return Optional.ofNullable(platformOpsMapper.findDatabaseOperation(operationId))
                .map(this::mapDatabaseOperation);
    }

    @Override
    public List<DatabaseOperation> findDatabaseOperations() {
        return platformOpsMapper.findDatabaseOperations().stream().map(this::mapDatabaseOperation).toList();
    }

    @Override
    public FinOpsAction saveFinOpsAction(FinOpsAction action) {
        platformOpsMapper.saveFinOpsAction(action);
        return action;
    }

    @Override
    public Optional<FinOpsAction> findFinOpsAction(long actionId) {
        return Optional.ofNullable(platformOpsMapper.findFinOpsAction(actionId)).map(this::mapFinOpsAction);
    }

    @Override
    public List<FinOpsAction> findFinOpsActions() {
        return platformOpsMapper.findFinOpsActions().stream().map(this::mapFinOpsAction).toList();
    }

    @Override
    public SecurityOperation saveSecurityOperation(SecurityOperation operation) {
        platformOpsMapper.saveSecurityOperation(operation);
        return operation;
    }

    @Override
    public Optional<SecurityOperation> findSecurityOperation(long operationId) {
        return Optional.ofNullable(platformOpsMapper.findSecurityOperation(operationId))
                .map(this::mapSecurityOperation);
    }

    @Override
    public List<SecurityOperation> findSecurityOperations() {
        return platformOpsMapper.findSecurityOperations().stream().map(this::mapSecurityOperation).toList();
    }

    private BackupPlan mapBackupPlan(Map<String, Object> row) {
        return new BackupPlan(longValue(row, "plan_id"), stringValue(row, "database_name"),
                stringValue(row, "backup_type"), intValue(row, "retention_days"),
                OpsStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private DatabaseOperation mapDatabaseOperation(Map<String, Object> row) {
        return new DatabaseOperation(longValue(row, "operation_id"), stringValue(row, "database_name"),
                stringValue(row, "operation_type"), RiskLevel.valueOf(stringValue(row, "risk_level")),
                OpsStatus.valueOf(stringValue(row, "status")), stringValue(row, "detail"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private FinOpsAction mapFinOpsAction(Map<String, Object> row) {
        return new FinOpsAction(longValue(row, "action_id"), stringValue(row, "service_name"),
                stringValue(row, "action_type"), decimalValue(row, "estimated_saving"),
                OpsStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private SecurityOperation mapSecurityOperation(Map<String, Object> row) {
        return new SecurityOperation(longValue(row, "operation_id"), stringValue(row, "service_name"),
                stringValue(row, "signal_type"), RiskLevel.valueOf(stringValue(row, "risk_level")),
                OpsStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }
}
