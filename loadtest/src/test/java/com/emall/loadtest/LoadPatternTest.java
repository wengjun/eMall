package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LoadPatternTest {
    @Test
    void shouldExposeFaultAndRecoveryWindows() {
        Duration total = Duration.ofSeconds(100);

        assertThat(LoadPattern.FAULT_RECOVERY.stageAt(Duration.ofSeconds(10), total, 1_000).name())
                .isEqualTo("baseline");
        assertThat(LoadPattern.FAULT_RECOVERY.stageAt(Duration.ofSeconds(40), total, 1_000).faultWindow()).isTrue();
        assertThat(LoadPattern.FAULT_RECOVERY.stageAt(Duration.ofSeconds(80), total, 1_000).name())
                .isEqualTo("recovery");
    }

    @Test
    void shouldIncreaseBreakpointRateByStage() {
        assertThat(LoadPattern.BREAKPOINT.stages(1_000)).extracting(LoadPattern.StageDefinition::globalRate)
                .containsExactly(200, 400, 600, 800, 1_000);
        assertThat(LoadPattern.BREAKPOINT.stages(1_000)).allMatch(LoadPattern.StageDefinition::checkpoint);
    }
}
