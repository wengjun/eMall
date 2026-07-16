package com.emall.traffic;

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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TrafficServiceTest {
    private final InMemoryTrafficRepository repository = new InMemoryTrafficRepository();
    private final ControlPlaneClient controlPlaneClient = mock(ControlPlaneClient.class);
    private final TrafficControlPlanePublisher publisher =
            new TrafficControlPlanePublisher(controlPlaneClient, new ControlPlaneProperties());
    private final TrafficService service =
            new TrafficService(repository, new SnowflakeIdGenerator(61L), publisher, controlPlaneClient);

    @Test
    void managesUnitsShardRoutesAndTrafficShifts() {
        service.registerUnit("unit-a", "east", 100);
        service.registerUnit("unit-b", "east", 100);
        service.routeShard("order", 1, "unit-a", "order-db-1");
        TrafficShift shift = service.planShift("unit-a", "unit-b", 30, "failover drill");
        service.changeShiftStatus(shift.shiftId(), ShiftStatus.RUNNING);
        service.isolateUnit("unit-a");

        TrafficSummary summary = service.summary();

        assertThat(summary.shardRoutes()).isEqualTo(1);
        assertThat(summary.runningShifts()).isEqualTo(1);
        assertThat(summary.isolatedUnits()).isEqualTo(1);
        ArgumentCaptor<ControlPlaneCommand> commands = ArgumentCaptor.forClass(ControlPlaneCommand.class);
        verify(controlPlaneClient, atLeastOnce()).submit(commands.capture());
        assertThat(commands.getAllValues()).extracting(ControlPlaneCommand::target)
                .contains(ControlPlaneTarget.NACOS_CONFIG, ControlPlaneTarget.INFRASTRUCTURE_API);
        assertThatThrownBy(() -> service.changeShiftStatus(shift.shiftId(), ShiftStatus.COMPLETED))
                .isInstanceOf(BusinessException.class).hasMessageContaining("has not been submitted");
    }

    @Test
    void managesDynamicControlRules() {
        service.registerUnit("unit-a", "east", 100);

        TrafficControlRule rule = service.upsertControlRule("flash-sale.enqueue", ControlRuleType.RATE_LIMIT,
                "campaign", "90001", 100000, "unit-a", true);
        service.changeControlRule(rule.ruleId(), false);

        assertThat(service.controlRules()).singleElement().satisfies(controlRule -> {
            assertThat(controlRule.resource()).isEqualTo("flash-sale.enqueue");
            assertThat(controlRule.type()).isEqualTo(ControlRuleType.RATE_LIMIT);
            assertThat(controlRule.enabled()).isFalse();
        });
        assertThat(service.summary().controlRules()).isEqualTo(1);
    }
}
