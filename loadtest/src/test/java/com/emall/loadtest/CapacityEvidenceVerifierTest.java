package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CapacityEvidenceVerifierTest {
    private static final List<String> PATTERNS =
            List.of("constant", "step", "spike", "soak", "fault_recovery", "breakpoint");

    @Test
    void shouldRequireEveryPatternAndRepeatedStablePreproductionRuns() {
        List<CapacityReport> reports = new ArrayList<>();
        for (String pattern : PATTERNS) {
            reports.add(report(pattern + "-1", pattern, 1_000.0));
            reports.add(report(pattern + "-2", pattern, 1_020.0));
        }

        CapacityEvidence evidence = new CapacityEvidenceVerifier().verify(reports, 2);

        assertThat(evidence.status()).isEqualTo("VERIFIED");
        assertThat(evidence.coveredPatterns()).containsExactlyInAnyOrderElementsOf(PATTERNS);
        assertThat(evidence.baselineQpsCoefficientOfVariation()).isLessThan(0.10);
    }

    @Test
    void shouldNotVerifyMissingOrIneligibleEvidence() {
        CapacityReport baseline = report("constant-1", "constant", 1_000.0);

        CapacityEvidence evidence = new CapacityEvidenceVerifier().verify(List.of(baseline), 2);

        assertThat(evidence.status()).isEqualTo("UNVERIFIED");
        assertThat(evidence.reasons()).anyMatch(reason -> reason.startsWith("missing test patterns"));
        assertThat(evidence.reasons()).contains("constant has 1 runs; 2 required");
    }

    private CapacityReport report(String runId, String pattern, double qps) {
        CapacityReport.AggregateMetrics metrics = new CapacityReport.AggregateMetrics(1_000L, 1_000L, 1_000L, 0L, 0L,
                0.0, qps, 10_000L, 20_000L, 30_000L, 40_000L, 100, 1_000L, 1_000L, 0L, 0L, 0L, 0L);
        CapacityReport.GeneratorSummary generator =
                new CapacityReport.GeneratorSummary(0.4, 100L, 1_000L, 20, 8, 1L, false, List.of());
        WorkerReport.RunMetadata metadata = new WorkerReport.RunMetadata("preprod-a", "preproduction",
                "0123456789abcdef0123456789abcdef01234567", "single-cell", "8C16G", 4, "per-user-fixture");
        WorkerReport.DataModel dataModel =
                new WorkerReport.DataModel(1_000_000L, 100_000L, 100_000, 10_000, 20, "read-heavy:80,checkout:20");
        WorkerReport.Thresholds thresholds = new WorkerReport.Thresholds(0.01, 100_000L, 250_000L, 0.85);
        CapacityReport.CapacityModel model = new CapacityReport.CapacityModel(qps, qps * 0.7, 100, 0.8, 0.7, qps * 56.0,
                30.0, 1_000_000L, qps * 1_680.0, true, "test formula");
        return new CapacityReport(1, runId, "PREPRODUCTION_RUN_ELIGIBLE", List.of(), "checkout", pattern,
                "2026-01-01T00:00:00Z", "2026-01-01T00:10:00Z", 4, 4, true, 1_000, 2_000, metrics, "", List.of(),
                generator, metadata, dataModel, Map.of(), thresholds, model,
                pattern.equals("fault_recovery") ? "chaos-1" : "");
    }
}
