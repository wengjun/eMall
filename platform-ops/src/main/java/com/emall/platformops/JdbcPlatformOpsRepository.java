package com.emall.platformops;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcPlatformOpsRepository implements PlatformOpsRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcPlatformOpsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public BackupPlan saveBackupPlan(BackupPlan plan) {
        jdbcTemplate.update("""
                INSERT INTO backup_plan
                    (plan_id, database_name, backup_type, retention_days, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, plan.planId(), plan.databaseName(), plan.backupType(), plan.retentionDays(), plan.status().name(),
                Timestamp.from(plan.createdAt()), Timestamp.from(plan.updatedAt()));
        return plan;
    }

    @Override
    public Optional<BackupPlan> findBackupPlan(long planId) {
        return jdbcTemplate.query("SELECT * FROM backup_plan WHERE plan_id = ?", this::mapBackupPlan, planId).stream()
                .findFirst();
    }

    @Override
    public List<BackupPlan> findBackupPlans() {
        return jdbcTemplate.query("SELECT * FROM backup_plan", this::mapBackupPlan);
    }

    @Override
    public DatabaseOperation saveDatabaseOperation(DatabaseOperation operation) {
        jdbcTemplate.update("""
                INSERT INTO database_operation
                    (operation_id, database_name, operation_type, risk_level, status, detail, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, operation.operationId(), operation.databaseName(), operation.operationType(),
                operation.riskLevel().name(), operation.status().name(), operation.detail(),
                Timestamp.from(operation.createdAt()), Timestamp.from(operation.updatedAt()));
        return operation;
    }

    @Override
    public Optional<DatabaseOperation> findDatabaseOperation(long operationId) {
        return jdbcTemplate.query("SELECT * FROM database_operation WHERE operation_id = ?", this::mapDatabaseOperation,
                operationId).stream().findFirst();
    }

    @Override
    public List<DatabaseOperation> findDatabaseOperations() {
        return jdbcTemplate.query("SELECT * FROM database_operation", this::mapDatabaseOperation);
    }

    @Override
    public FinOpsAction saveFinOpsAction(FinOpsAction action) {
        jdbcTemplate.update("""
                INSERT INTO finops_action
                    (action_id, service_name, action_type, estimated_saving, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, action.actionId(), action.serviceName(), action.actionType(), action.estimatedSaving(),
                action.status().name(), Timestamp.from(action.createdAt()), Timestamp.from(action.updatedAt()));
        return action;
    }

    @Override
    public Optional<FinOpsAction> findFinOpsAction(long actionId) {
        return jdbcTemplate.query("SELECT * FROM finops_action WHERE action_id = ?", this::mapFinOpsAction, actionId)
                .stream().findFirst();
    }

    @Override
    public List<FinOpsAction> findFinOpsActions() {
        return jdbcTemplate.query("SELECT * FROM finops_action", this::mapFinOpsAction);
    }

    @Override
    public SecurityOperation saveSecurityOperation(SecurityOperation operation) {
        jdbcTemplate.update("""
                INSERT INTO security_operation
                    (operation_id, service_name, signal_type, risk_level, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, operation.operationId(), operation.serviceName(), operation.signalType(),
                operation.riskLevel().name(), operation.status().name(), Timestamp.from(operation.createdAt()),
                Timestamp.from(operation.updatedAt()));
        return operation;
    }

    @Override
    public Optional<SecurityOperation> findSecurityOperation(long operationId) {
        return jdbcTemplate.query("SELECT * FROM security_operation WHERE operation_id = ?", this::mapSecurityOperation,
                operationId).stream().findFirst();
    }

    @Override
    public List<SecurityOperation> findSecurityOperations() {
        return jdbcTemplate.query("SELECT * FROM security_operation", this::mapSecurityOperation);
    }

    private BackupPlan mapBackupPlan(ResultSet rs, int rowNum) throws SQLException {
        return new BackupPlan(rs.getLong("plan_id"), rs.getString("database_name"), rs.getString("backup_type"),
                rs.getInt("retention_days"), OpsStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private DatabaseOperation mapDatabaseOperation(ResultSet rs, int rowNum) throws SQLException {
        return new DatabaseOperation(rs.getLong("operation_id"), rs.getString("database_name"),
                rs.getString("operation_type"), RiskLevel.valueOf(rs.getString("risk_level")),
                OpsStatus.valueOf(rs.getString("status")), rs.getString("detail"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private FinOpsAction mapFinOpsAction(ResultSet rs, int rowNum) throws SQLException {
        return new FinOpsAction(rs.getLong("action_id"), rs.getString("service_name"), rs.getString("action_type"),
                rs.getBigDecimal("estimated_saving"), OpsStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private SecurityOperation mapSecurityOperation(ResultSet rs, int rowNum) throws SQLException {
        return new SecurityOperation(rs.getLong("operation_id"), rs.getString("service_name"),
                rs.getString("signal_type"), RiskLevel.valueOf(rs.getString("risk_level")),
                OpsStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
