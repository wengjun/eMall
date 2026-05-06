package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ChaosDrillCatalogTest {
    private final ChaosSafetyGate safetyGate = new ChaosSafetyGate();

    @Test
    void p3CatalogCoversRequiredDrillTypes() {
        Set<DrillType> drillTypes = ChaosDrillCatalog.p3Baseline().stream()
                .map(ChaosDrill::type)
                .collect(Collectors.toSet());

        assertThat(drillTypes).isEqualTo(EnumSet.allOf(DrillType.class));
    }

    @Test
    void p3CatalogPassesSafetyGate() {
        assertThat(ChaosDrillCatalog.p3Baseline())
                .allSatisfy(drill -> assertThat(safetyGate.validate(drill).approved()).isTrue());
    }

    @Test
    void safetyGateRejectsOverBroadOrUnboundedDrills() {
        ChaosDrill unsafe = new ChaosDrill("unsafe", DrillType.PARTIAL_REGION_ISOLATION, "global",
                BlastRadius.CROSS_REGION, Duration.ofMinutes(45), java.util.List.of("SLO dashboard open"),
                java.util.List.of("blackhole global traffic"), java.util.List.of(), java.util.List.of());

        ChaosSafetyReport report = safetyGate.validate(unsafe);

        assertThat(report.approved()).isFalse();
        assertThat(report.violations())
                .contains("duration exceeds 30 minutes")
                .contains("cross-region blast radius requires manual executive approval")
                .contains("abort conditions are required")
                .contains("recovery checks are required")
                .contains("rollback owner prerequisite is required");
    }
}
