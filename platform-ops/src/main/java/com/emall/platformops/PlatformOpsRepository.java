package com.emall.platformops;

import java.util.List;
import java.util.Optional;

interface PlatformOpsRepository {
    BackupPlan saveBackupPlan(BackupPlan plan);

    Optional<BackupPlan> findBackupPlan(long planId);

    List<BackupPlan> findBackupPlans();

    DatabaseOperation saveDatabaseOperation(DatabaseOperation operation);

    Optional<DatabaseOperation> findDatabaseOperation(long operationId);

    List<DatabaseOperation> findDatabaseOperations();

    FinOpsAction saveFinOpsAction(FinOpsAction action);

    Optional<FinOpsAction> findFinOpsAction(long actionId);

    List<FinOpsAction> findFinOpsActions();

    SecurityOperation saveSecurityOperation(SecurityOperation operation);

    Optional<SecurityOperation> findSecurityOperation(long operationId);

    List<SecurityOperation> findSecurityOperations();
}
