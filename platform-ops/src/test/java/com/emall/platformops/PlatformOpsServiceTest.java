package com.emall.platformops;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PlatformOpsServiceTest {
    private final InMemoryPlatformOpsRepository repository = new InMemoryPlatformOpsRepository();
    private final PlatformOpsService service = new PlatformOpsService(repository, new SnowflakeIdGenerator(64L));

    @Test
    void managesBackupDatabaseFinOpsAndSecurityOperations() {
        service.createBackupPlan("order", "pitr", 30);
        DatabaseOperation operation = service.createDatabaseOperation("order", "online-ddl", RiskLevel.HIGH,
                "add covering index");
        service.changeDatabaseOperationStatus(operation.operationId(), OpsStatus.BLOCKED);
        FinOpsAction action = service.createFinOpsAction("search", "right-size-index", new BigDecimal("1200.00"));
        service.approveFinOpsAction(action.actionId());
        service.createSecurityOperation("payment", "credential-rotation", RiskLevel.CRITICAL);

        PlatformOpsSummary summary = service.summary();

        assertThat(summary.backupPlans()).isEqualTo(1);
        assertThat(summary.blockedDatabaseOps()).isEqualTo(1);
        assertThat(summary.approvedFinOpsActions()).isEqualTo(1);
        assertThat(summary.criticalSecuritySignals()).isEqualTo(1);
    }
}
