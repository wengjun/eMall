package com.emall.platformops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emall.common.controlplane.ControlPlaneCommand;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.controlplane.ControlPlaneClient;
import com.emall.common.controlplane.ControlPlaneTarget;
import com.emall.common.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlatformOpsServiceTest {
    private final InMemoryPlatformOpsRepository repository = new InMemoryPlatformOpsRepository();
    private final ControlPlaneClient controlPlaneClient = mock(ControlPlaneClient.class);
    private final PlatformControlPlanePublisher publisher = new PlatformControlPlanePublisher(controlPlaneClient);
    private final PlatformOpsService service =
            new PlatformOpsService(repository, new SnowflakeIdGenerator(64L), publisher, controlPlaneClient);

    @Test
    void managesBackupDatabaseFinOpsAndSecurityOperations() {
        BackupPlan backup = service.createBackupPlan("order", "pitr", 30);
        DatabaseOperation operation =
                service.createDatabaseOperation("order", "online-ddl", RiskLevel.HIGH, "add covering index");
        service.changeDatabaseOperationStatus(operation.operationId(), OpsStatus.BLOCKED);
        FinOpsAction action = service.createFinOpsAction("search", "right-size-index", new BigDecimal("1200.00"));
        service.approveFinOpsAction(action.actionId());
        service.createSecurityOperation("payment", "credential-rotation", RiskLevel.CRITICAL);

        PlatformOpsSummary summary = service.summary();

        assertThat(summary.backupPlans()).isEqualTo(1);
        assertThat(summary.blockedDatabaseOps()).isEqualTo(1);
        assertThat(summary.approvedFinOpsActions()).isEqualTo(1);
        assertThat(summary.criticalSecuritySignals()).isEqualTo(1);
        ArgumentCaptor<ControlPlaneCommand> commands = ArgumentCaptor.forClass(ControlPlaneCommand.class);
        verify(controlPlaneClient, atLeastOnce()).submit(commands.capture());
        assertThat(commands.getAllValues())
                .allMatch(command -> command.target() == ControlPlaneTarget.INFRASTRUCTURE_API);
        assertThatThrownBy(() -> service.changeBackupStatus(backup.planId(), OpsStatus.COMPLETED))
                .isInstanceOf(BusinessException.class).hasMessageContaining("has not been submitted");
    }
}
