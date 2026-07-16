package com.emall.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emall.common.controlplane.ControlPlaneCommand;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.controlplane.ControlPlaneClient;
import com.emall.common.controlplane.ControlPlaneProperties;
import com.emall.common.controlplane.ControlPlaneTarget;
import com.emall.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReliabilityServiceTest {
    private final InMemoryReliabilityRepository repository = new InMemoryReliabilityRepository();
    private final ControlPlaneClient controlPlaneClient = mock(ControlPlaneClient.class);
    private final ReliabilityControlPlanePublisher publisher =
            new ReliabilityControlPlanePublisher(controlPlaneClient, new ControlPlaneProperties());
    private final ReliabilityService service =
            new ReliabilityService(repository, new SnowflakeIdGenerator(62L), publisher, controlPlaneClient);

    @Test
    void managesCapacitySloChaosAndReadiness() {
        CapacityRehearsal rehearsal = service.createRehearsal("order", 100000, 10000);
        service.defineSlo("order", new BigDecimal("0.999900"), 300, new BigDecimal("0.010000"));
        ChaosSchedule chaos = service.scheduleChaos("order", "database-latency", 5, Instant.now().plusSeconds(3600));
        service.approveChaos(chaos.chaosId());
        service.evaluateReadiness("order", true, true, false);

        ReliabilitySummary summary = service.summary();

        assertThat(summary.rehearsals()).isEqualTo(1);
        assertThat(summary.approvedChaos()).isEqualTo(1);
        assertThat(summary.blockedReadinessGates()).isEqualTo(1);
        assertThat(summary.sloObjectives()).isEqualTo(1);
        ArgumentCaptor<ControlPlaneCommand> commands = ArgumentCaptor.forClass(ControlPlaneCommand.class);
        verify(controlPlaneClient, atLeastOnce()).submit(commands.capture());
        assertThat(commands.getAllValues()).extracting(ControlPlaneCommand::target)
                .contains(ControlPlaneTarget.INFRASTRUCTURE_API, ControlPlaneTarget.KUBERNETES_RESOURCE);
        assertThatThrownBy(() -> service.changeRehearsalStatus(rehearsal.rehearsalId(), GateStatus.PASSED))
                .isInstanceOf(BusinessException.class).hasMessageContaining("has not been submitted");
    }
}
