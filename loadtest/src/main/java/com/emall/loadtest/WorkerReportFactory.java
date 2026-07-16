package com.emall.loadtest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class WorkerReportFactory {
    private WorkerReportFactory() {
    }

    static WorkerReport create(LoadTestOptions options, StreamingLoadMetrics.Snapshot snapshot,
            SystemResourceSampler.Usage usage, Instant startedAt, Instant finishedAt, Duration elapsed) {
        double seconds = Math.max(0.001, elapsed.toNanos() / 1_000_000_000.0);
        double measuredQps = snapshot.success() / seconds;
        List<String> generatorReasons = generatorReasons(options, snapshot, usage);
        WorkerReport.RequestMetrics requestMetrics = new WorkerReport.RequestMetrics(snapshot.attempted(),
                snapshot.completed(), snapshot.success(), snapshot.failed(), snapshot.backpressureRejected(),
                snapshot.errorRate(), measuredQps, snapshot.percentile(50.0), snapshot.percentile(95.0),
                snapshot.percentile(99.0), snapshot.histogram().getMaxValue(), snapshot.peakInflight(),
                snapshot.maxSchedulerLagMicros(), snapshot.status2xx(), snapshot.status4xx(), snapshot.status429(),
                snapshot.status5xx(), snapshot.transportErrors());
        WorkerReport.GeneratorMetrics generator = new WorkerReport.GeneratorMetrics(usage.processCpuUtilization(),
                usage.heapUsedBytes(), usage.heapMaxBytes(), usage.peakThreads(), usage.availableProcessors(),
                usage.gcPauseMillis(), !generatorReasons.isEmpty(), List.copyOf(generatorReasons));
        WorkerReport.RunMetadata metadata = new WorkerReport.RunMetadata(options.environment(), options.evidenceScope(),
                options.gitCommit(), options.deployment(), options.targetResources(), options.serviceInstances(),
                authenticationModel(options));
        WorkerReport.DataModel dataModel = new WorkerReport.DataModel(options.datasetUsers(), options.datasetSkus(),
                options.userCardinality(), options.skuCardinality(), options.hotSkuPercent(), options.trafficMix());
        WorkerReport.Thresholds thresholds = new WorkerReport.Thresholds(options.maxErrorRate(), options.maxP95Micros(),
                options.maxSchedulerLag().toNanos() / 1_000L, options.maxGeneratorCpu());
        WorkerReport.CapacityInputs capacityInputs =
                new WorkerReport.CapacityInputs(options.cellCount(), options.scalingEfficiency(),
                        options.capacityHeadroom(), options.targetPeakConcurrency(), options.sessionThinkSeconds());

        return new WorkerReport(1, options.runId(), options.worker().index(), options.worker().count(),
                options.role().name().toLowerCase(), options.scenario().cliName(),
                options.pattern().name().toLowerCase(), startedAt.toString(), finishedAt.toString(), elapsed.toMillis(),
                options.ratePerSecond(), options.localRate(options.ratePerSecond()), options.maxInflight(),
                requestMetrics, HistogramCodec.encode(snapshot.histogram()), stageReports(options, snapshot.stages()),
                generator, metadata, dataModel, SaturationMetricsReader.read(options.saturationFile()), thresholds,
                capacityInputs, options.faultExperiment());
    }

    private static List<WorkerReport.StageReport> stageReports(LoadTestOptions options,
            List<StreamingLoadMetrics.StageSnapshot> snapshots) {
        Map<String, StreamingLoadMetrics.StageSnapshot> byName = snapshots.stream()
                .collect(Collectors.toMap(StreamingLoadMetrics.StageSnapshot::name, Function.identity()));
        List<WorkerReport.StageReport> reports = new ArrayList<>();
        for (LoadPattern.StageDefinition stage : options.pattern().stages(options.ratePerSecond())) {
            StreamingLoadMetrics.StageSnapshot snapshot = byName.get(stage.name());
            if (snapshot != null) {
                reports.add(new WorkerReport.StageReport(stage.name(), stage.globalRate(), stage.faultWindow(),
                        stage.checkpoint(), snapshot.completed(), snapshot.success(), snapshot.failed(),
                        snapshot.histogram(), snapshot.p50Micros(), snapshot.p95Micros(), snapshot.p99Micros()));
            }
        }
        return List.copyOf(reports);
    }

    private static List<String> generatorReasons(LoadTestOptions options, StreamingLoadMetrics.Snapshot snapshot,
            SystemResourceSampler.Usage usage) {
        List<String> reasons = new ArrayList<>();
        if (snapshot.backpressureRejected() > 0L) {
            reasons.add("in-flight backpressure rejected requests");
        }
        if (usage.processCpuUtilization() >= 0.0 && usage.processCpuUtilization() > options.maxGeneratorCpu()) {
            reasons.add("load-generator CPU exceeded threshold");
        }
        if (snapshot.maxSchedulerLagMicros() > options.maxSchedulerLag().toNanos() / 1_000L) {
            reasons.add("request scheduler lag exceeded threshold");
        }
        if (usage.heapMaxBytes() > 0L && (double) usage.heapUsedBytes() / usage.heapMaxBytes() > 0.90) {
            reasons.add("load-generator heap utilization exceeded 90%");
        }
        double expectedRate = options.pattern().expectedAverageLocalRate(options);
        double offeredRate = snapshot.attempted() / Math.max(0.001, options.duration().toNanos() / 1_000_000_000.0);
        if (expectedRate > 0.0 && offeredRate < expectedRate * 0.90) {
            reasons.add("offered request rate was below 90% of target");
        }
        return reasons;
    }

    private static String authenticationModel(LoadTestOptions options) {
        if (options.identityFixtureFile() != null) {
            return "per-user-fixture";
        }
        return options.authToken().isBlank() ? "none" : "shared-token";
    }

}
