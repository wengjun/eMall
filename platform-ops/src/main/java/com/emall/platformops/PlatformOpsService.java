package com.emall.platformops;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PlatformOpsService {
    private final PlatformOpsRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    PlatformOpsService(PlatformOpsRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    BackupPlan createBackupPlan(String databaseName, String backupType, int retentionDays) {
        if (retentionDays <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "retention days must be positive");
        }
        Instant now = Instant.now();
        return repository.saveBackupPlan(new BackupPlan(idGenerator.nextId(), normalize(databaseName),
                normalize(backupType), retentionDays, OpsStatus.OPEN, now, now));
    }

    @Transactional
    BackupPlan changeBackupStatus(long planId, OpsStatus status) {
        BackupPlan plan = repository.findBackupPlan(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "backup plan not found"));
        return repository.saveBackupPlan(plan.changeStatus(status));
    }

    @Transactional
    DatabaseOperation createDatabaseOperation(String databaseName, String operationType, RiskLevel riskLevel,
            String detail) {
        Instant now = Instant.now();
        return repository.saveDatabaseOperation(new DatabaseOperation(idGenerator.nextId(), normalize(databaseName),
                normalize(operationType), riskLevel, OpsStatus.OPEN, normalize(detail), now, now));
    }

    @Transactional
    DatabaseOperation changeDatabaseOperationStatus(long operationId, OpsStatus status) {
        DatabaseOperation operation = repository.findDatabaseOperation(operationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "database operation not found"));
        return repository.saveDatabaseOperation(operation.changeStatus(status));
    }

    @Transactional
    FinOpsAction createFinOpsAction(String serviceName, String actionType, BigDecimal estimatedSaving) {
        if (estimatedSaving.signum() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "estimated saving must be non-negative");
        }
        Instant now = Instant.now();
        return repository.saveFinOpsAction(new FinOpsAction(idGenerator.nextId(), normalize(serviceName),
                normalize(actionType), estimatedSaving, OpsStatus.OPEN, now, now));
    }

    @Transactional
    FinOpsAction approveFinOpsAction(long actionId) {
        FinOpsAction action = repository.findFinOpsAction(actionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "finops action not found"));
        return repository.saveFinOpsAction(action.changeStatus(OpsStatus.APPROVED));
    }

    @Transactional
    SecurityOperation createSecurityOperation(String serviceName, String signalType, RiskLevel riskLevel) {
        Instant now = Instant.now();
        return repository.saveSecurityOperation(new SecurityOperation(idGenerator.nextId(), normalize(serviceName),
                normalize(signalType), riskLevel, OpsStatus.OPEN, now, now));
    }

    @Transactional
    SecurityOperation changeSecurityStatus(long operationId, OpsStatus status) {
        SecurityOperation operation = repository.findSecurityOperation(operationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "security operation not found"));
        return repository.saveSecurityOperation(operation.changeStatus(status));
    }

    PlatformOpsSummary summary() {
        int blockedDatabaseOps = (int) repository.findDatabaseOperations().stream()
                .filter(operation -> operation.status() == OpsStatus.BLOCKED).count();
        int approvedFinOps = (int) repository.findFinOpsActions().stream()
                .filter(action -> action.status() == OpsStatus.APPROVED).count();
        int criticalSecurity = (int) repository.findSecurityOperations().stream().filter(
                operation -> operation.riskLevel() == RiskLevel.CRITICAL && operation.status() != OpsStatus.COMPLETED)
                .count();
        return new PlatformOpsSummary(repository.findBackupPlans().size(), blockedDatabaseOps, approvedFinOps,
                criticalSecurity);
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "platform ops value must not be blank");
        }
        return normalized;
    }
}
