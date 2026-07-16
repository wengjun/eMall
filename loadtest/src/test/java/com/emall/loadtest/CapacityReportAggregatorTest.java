package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.HdrHistogram.Histogram;
import org.junit.jupiter.api.Test;

class CapacityReportAggregatorTest {
    @Test
    void shouldMergeWorkerHistogramsAndProduceEligiblePreproductionRun() {
        CapacityReport report =
                new CapacityReportAggregator().aggregate(List.of(worker(0, 2, 1_000L), worker(1, 2, 100_000L)));

        assertThat(report.workerSetComplete()).isTrue();
        assertThat(report.metrics().success()).isEqualTo(2L);
        assertThat(report.metrics().p99Micros()).isGreaterThanOrEqualTo(100_000L);
        assertThat(report.status()).isEqualTo("PREPRODUCTION_RUN_ELIGIBLE");
        assertThat(report.reasons()).isEmpty();
    }

    @Test
    void shouldInvalidateIncompleteWorkerSet() {
        CapacityReport report = new CapacityReportAggregator().aggregate(List.of(worker(0, 2, 1_000L)));

        assertThat(report.workerSetComplete()).isFalse();
        assertThat(report.status()).isEqualTo("INVALID");
        assertThat(report.reasons()).contains("worker report set is incomplete");
    }

    @Test
    void shouldRejectDuplicateWorkerIndexes() {
        assertThatThrownBy(
                () -> new CapacityReportAggregator().aggregate(List.of(worker(0, 2, 1_000L), worker(0, 2, 1_000L))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicate worker index");
    }

    @Test
    void shouldInvalidateAReportWhenGeneratorNetworkIsSaturated() {
        CapacityReport report = new CapacityReportAggregator().aggregate(List.of(worker(0, 1, 1_000L)),
                Map.of("generator.network.utilization", 0.90));

        assertThat(report.status()).isEqualTo("INVALID");
        assertThat(report.generator().reasons()).contains("load-generator network utilization exceeded 85%");
    }

    static WorkerReport worker(int index, int count, long latencyMicros) {
        Histogram histogram = new Histogram(3);
        histogram.recordValue(latencyMicros);
        WorkerReport.RequestMetrics metrics = new WorkerReport.RequestMetrics(1L, 1L, 1L, 0L, 0L, 0.0, 1.0,
                latencyMicros, latencyMicros, latencyMicros, latencyMicros, 1, 1L, 1L, 0L, 0L, 0L, 0L);
        WorkerReport.StageReport stage = new WorkerReport.StageReport("steady", 2, false, false, 1L, 1L, 0L,
                HistogramCodec.encode(histogram), latencyMicros, latencyMicros, latencyMicros);
        WorkerReport.GeneratorMetrics generator =
                new WorkerReport.GeneratorMetrics(0.20, 100L, 1_000L, 10, 4, 1L, false, List.of());
        WorkerReport.RunMetadata metadata =
                new WorkerReport.RunMetadata("preprod-a", "preproduction", "0123456789abcdef0123456789abcdef01234567",
                        "single-cell", "gateway=2x8C16G;mysql=8C32G", 2, "per-user-fixture");
        WorkerReport.DataModel dataModel =
                new WorkerReport.DataModel(1_000_000L, 100_000L, 100_000, 10_000, 20, "read-heavy:80,checkout:20");
        WorkerReport.Thresholds thresholds = new WorkerReport.Thresholds(0.01, 200_000L, 250_000L, 0.85);
        WorkerReport.CapacityInputs inputs = new WorkerReport.CapacityInputs(100, 0.8, 0.7, 1L, 30.0);
        return new WorkerReport(1, "aggregate-test", index, count, "worker", "checkout", "constant",
                "2026-01-01T00:00:00Z", "2026-01-01T00:00:01Z", 1_000L, 2, 1, 2, metrics,
                HistogramCodec.encode(histogram), List.of(stage), generator, metadata, dataModel, saturation(),
                thresholds, inputs, "");
    }

    private static Map<String, Double> saturation() {
        Map<String, Double> values = new java.util.LinkedHashMap<>();
        new ArrayList<>(CapacityReportAggregator.REQUIRED_SATURATION_METRICS).forEach(name -> values.put(name, 0.50));
        return Map.copyOf(values);
    }
}
