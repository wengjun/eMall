package com.emall.traffic;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;

class TrafficServiceTest {
    private final InMemoryTrafficRepository repository = new InMemoryTrafficRepository();
    private final TrafficService service = new TrafficService(repository, new SnowflakeIdGenerator(61L));

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
    }
}
