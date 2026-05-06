package com.emall.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReliabilityServiceTest {
    private final InMemoryReliabilityRepository repository = new InMemoryReliabilityRepository();
    private final ReliabilityService service = new ReliabilityService(repository, new SnowflakeIdGenerator(62L));

    @Test
    void managesCapacitySloChaosAndReadiness() {
        CapacityRehearsal rehearsal = service.createRehearsal("order", 100000, 10000);
        service.changeRehearsalStatus(rehearsal.rehearsalId(), GateStatus.PASSED);
        service.defineSlo("order", new BigDecimal("0.999900"), 300, new BigDecimal("0.010000"));
        ChaosSchedule chaos = service.scheduleChaos("order", "database-latency", 5, Instant.now().plusSeconds(3600));
        service.approveChaos(chaos.chaosId());
        service.evaluateReadiness("order", true, true, false);

        ReliabilitySummary summary = service.summary();

        assertThat(summary.rehearsals()).isEqualTo(1);
        assertThat(summary.approvedChaos()).isEqualTo(1);
        assertThat(summary.blockedReadinessGates()).isEqualTo(1);
        assertThat(summary.sloObjectives()).isEqualTo(1);
    }
}
