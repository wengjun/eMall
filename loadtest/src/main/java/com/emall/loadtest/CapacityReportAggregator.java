package com.emall.loadtest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.HdrHistogram.Histogram;

final class CapacityReportAggregator {
    private static final double MAX_GENERATOR_NETWORK_UTILIZATION = 0.85;
    static final Set<String> REQUIRED_SATURATION_METRICS = Set.of("gateway.cpu.utilization",
            "application.cpu.utilization", "mysql.cpu.utilization", "mysql.connection.utilization",
            "redis.cpu.utilization", "kafka.consumer.lag", "generator.network.utilization");

    CapacityReport aggregate(List<WorkerReport> input) {
        return aggregate(input, Map.of());
    }

    CapacityReport aggregate(List<WorkerReport> input, Map<String, Double> coordinatorSaturation) {
        if (input.isEmpty()) {
            throw new IllegalArgumentException("at least one worker report is required");
        }
        List<WorkerReport> workers = input.stream().sorted(Comparator.comparingInt(WorkerReport::workerIndex)).toList();
        WorkerReport first = workers.get(0);
        validateCompatible(workers, first);
        boolean complete = completeWorkerSet(workers, first.workerCount());
        Histogram latency = mergeHistograms(workers.stream().map(WorkerReport::histogram).toList());
        CapacityReport.AggregateMetrics metrics = aggregateMetrics(workers, latency);
        CapacityReport.GeneratorSummary generator = aggregateGenerator(workers, metrics);
        Map<String, Double> mergedSaturation = aggregateSaturation(workers);
        coordinatorSaturation.forEach((name, value) -> mergedSaturation.merge(name, value, Math::max));
        Map<String, Double> saturation = Map.copyOf(mergedSaturation);
        generator = applyGeneratorNetworkSaturation(generator, saturation);
        List<CapacityReport.StageAggregate> stages = aggregateStages(workers);
        List<String> reasons = evidenceReasons(first, complete, generator, metrics, saturation, stages);
        CapacityReport.CapacityModel capacityModel = capacityModel(first, metrics, complete, generator.bottleneck());
        String status = status(complete, generator, reasons);

        String startedAt = workers.stream().map(WorkerReport::startedAt).map(Instant::parse).min(Instant::compareTo)
                .orElseThrow().toString();
        String finishedAt = workers.stream().map(WorkerReport::finishedAt).map(Instant::parse).max(Instant::compareTo)
                .orElseThrow().toString();
        return new CapacityReport(1, first.runId(), status, List.copyOf(reasons), first.scenario(), first.pattern(),
                startedAt, finishedAt, first.workerCount(), workers.size(), complete, first.globalTargetQps(),
                first.maxInflight(), metrics, HistogramCodec.encode(latency), stages, generator, first.metadata(),
                first.dataModel(), saturation, first.thresholds(), capacityModel, first.faultExperiment());
    }

    private void validateCompatible(List<WorkerReport> workers, WorkerReport first) {
        Set<Integer> indexes = new LinkedHashSet<>();
        for (WorkerReport worker : workers) {
            if (!indexes.add(worker.workerIndex())) {
                throw new IllegalArgumentException("duplicate worker index " + worker.workerIndex());
            }
            if (!worker.runId().equals(first.runId()) || worker.workerCount() != first.workerCount()
                    || worker.globalTargetQps() != first.globalTargetQps()
                    || !worker.scenario().equals(first.scenario()) || !worker.pattern().equals(first.pattern())
                    || !worker.thresholds().equals(first.thresholds())
                    || !worker.capacityInputs().equals(first.capacityInputs())
                    || !worker.metadata().equals(first.metadata()) || !worker.dataModel().equals(first.dataModel())) {
                throw new IllegalArgumentException("worker reports do not describe the same immutable test run");
            }
        }
    }

    private boolean completeWorkerSet(List<WorkerReport> workers, int expected) {
        if (workers.size() != expected) {
            return false;
        }
        for (int index = 0; index < expected; index++) {
            if (workers.get(index).workerIndex() != index) {
                return false;
            }
        }
        return true;
    }

    private CapacityReport.AggregateMetrics aggregateMetrics(List<WorkerReport> workers, Histogram latency) {
        long attempted = sum(workers, worker -> worker.metrics().attempted());
        long completed = sum(workers, worker -> worker.metrics().completed());
        long success = sum(workers, worker -> worker.metrics().success());
        long failed = sum(workers, worker -> worker.metrics().failed());
        long rejected = sum(workers, worker -> worker.metrics().backpressureRejected());
        double errorRate = attempted == 0L ? 0.0 : (double) (failed + rejected) / attempted;
        double durationSeconds =
                Math.max(0.001, workers.stream().mapToLong(WorkerReport::durationMillis).max().orElse(1L) / 1_000.0);
        return new CapacityReport.AggregateMetrics(attempted, completed, success, failed, rejected, errorRate,
                success / durationSeconds, latency.getValueAtPercentile(50.0), latency.getValueAtPercentile(95.0),
                latency.getValueAtPercentile(99.0), latency.getMaxValue(),
                workers.stream().mapToInt(worker -> worker.metrics().peakInflight()).sum(),
                workers.stream().mapToLong(worker -> worker.metrics().maxSchedulerLagMicros()).max().orElse(0L),
                sum(workers, worker -> worker.metrics().status2xx()),
                sum(workers, worker -> worker.metrics().status4xx()),
                sum(workers, worker -> worker.metrics().status429()),
                sum(workers, worker -> worker.metrics().status5xx()),
                sum(workers, worker -> worker.metrics().transportErrors()));
    }

    private CapacityReport.GeneratorSummary aggregateGenerator(List<WorkerReport> workers,
            CapacityReport.AggregateMetrics metrics) {
        List<String> reasons = workers.stream().flatMap(worker -> worker.generator().reasons().stream()
                .map(reason -> "worker-" + worker.workerIndex() + ": " + reason)).distinct().toList();
        return new CapacityReport.GeneratorSummary(
                workers.stream().mapToDouble(worker -> worker.generator().processCpuUtilization()).max().orElse(-1.0),
                sum(workers, worker -> worker.generator().heapUsedBytes()),
                sum(workers, worker -> worker.generator().heapMaxBytes()),
                workers.stream().mapToInt(worker -> worker.generator().peakThreads()).sum(),
                workers.stream().mapToInt(worker -> worker.generator().availableProcessors()).sum(),
                workers.stream().mapToLong(worker -> worker.generator().gcPauseMillis()).max().orElse(0L),
                workers.stream().anyMatch(worker -> worker.generator().bottleneck())
                        || metrics.backpressureRejected() > 0L,
                reasons);
    }

    private Map<String, Double> aggregateSaturation(List<WorkerReport> workers) {
        Map<String, Double> saturation = new LinkedHashMap<>();
        workers.forEach(worker -> worker.saturationMetrics()
                .forEach((name, value) -> saturation.merge(name, value, Math::max)));
        return saturation;
    }

    private CapacityReport.GeneratorSummary applyGeneratorNetworkSaturation(CapacityReport.GeneratorSummary generator,
            Map<String, Double> saturation) {
        double networkUtilization = saturation.getOrDefault("generator.network.utilization", 0.0);
        if (networkUtilization <= MAX_GENERATOR_NETWORK_UTILIZATION) {
            return generator;
        }
        List<String> reasons = new ArrayList<>(generator.reasons());
        reasons.add("load-generator network utilization exceeded 85%");
        return new CapacityReport.GeneratorSummary(generator.maxProcessCpuUtilization(), generator.totalHeapUsedBytes(),
                generator.totalHeapMaxBytes(), generator.totalPeakThreads(), generator.totalAvailableProcessors(),
                generator.maxGcPauseMillis(), true, List.copyOf(reasons));
    }

    private List<CapacityReport.StageAggregate> aggregateStages(List<WorkerReport> workers) {
        Map<String, List<WorkerReport.StageReport>> grouped =
                workers.stream().flatMap(worker -> worker.stages().stream())
                        .collect(Collectors.groupingBy(WorkerReport.StageReport::name));
        List<CapacityReport.StageAggregate> result = new ArrayList<>();
        grouped.forEach((name, reports) -> {
            WorkerReport.StageReport first = reports.get(0);
            Histogram histogram = mergeHistograms(reports.stream().map(WorkerReport.StageReport::histogram).toList());
            long completed = reports.stream().mapToLong(WorkerReport.StageReport::completed).sum();
            long success = reports.stream().mapToLong(WorkerReport.StageReport::success).sum();
            long failed = reports.stream().mapToLong(WorkerReport.StageReport::failed).sum();
            result.add(new CapacityReport.StageAggregate(name, first.targetQps(), first.faultWindow(),
                    first.checkpoint(), completed, success, failed, completed == 0L ? 0.0 : (double) failed / completed,
                    histogram.getValueAtPercentile(50.0), histogram.getValueAtPercentile(95.0),
                    histogram.getValueAtPercentile(99.0)));
        });
        return result.stream().sorted(Comparator.comparing(CapacityReport.StageAggregate::name)).toList();
    }

    private List<String> evidenceReasons(WorkerReport first, boolean complete,
            CapacityReport.GeneratorSummary generator, CapacityReport.AggregateMetrics metrics,
            Map<String, Double> saturation, List<CapacityReport.StageAggregate> stages) {
        List<String> reasons = new ArrayList<>();
        if (!complete) {
            reasons.add("worker report set is incomplete");
        }
        if (generator.bottleneck()) {
            reasons.add("load generator was a bottleneck");
        }
        if (!scenarioOutcomePassed(first, metrics, stages)) {
            reasons.add("scenario acceptance thresholds were not met");
        }
        if (!"preproduction".equalsIgnoreCase(first.metadata().evidenceScope())) {
            reasons.add("evidence scope is not preproduction");
        }
        if (!first.metadata().gitCommit().matches("[0-9a-fA-F]{40}")) {
            reasons.add("a full Git commit SHA is required");
        }
        if (first.metadata().targetResources().isBlank()
                || "unspecified".equalsIgnoreCase(first.metadata().targetResources())) {
            reasons.add("target resource inventory is missing");
        }
        if (first.dataModel().datasetUsers() <= 0L || first.dataModel().datasetSkus() <= 0L) {
            reasons.add("dataset scale is missing");
        }
        if (requiresPerUserCredentials(first) && first.dataModel().activeUserCardinality() > 1
                && !"per-user-fixture".equals(first.metadata().authenticationModel())) {
            reasons.add("multi-user write traffic requires per-user authentication fixtures");
        }
        Set<String> missingMetrics = new LinkedHashSet<>(REQUIRED_SATURATION_METRICS);
        missingMetrics.removeAll(saturation.keySet());
        if (!missingMetrics.isEmpty()) {
            reasons.add("missing saturation metrics: " + String.join(", ", missingMetrics));
        }
        if ("fault_recovery".equalsIgnoreCase(first.pattern()) && first.faultExperiment().isBlank()) {
            reasons.add("fault-recovery run is missing a fault experiment identifier");
        }
        return reasons;
    }

    private boolean requiresPerUserCredentials(WorkerReport report) {
        return switch (report.scenario()) {
            case "checkout", "hot-sku", "flash-sale-hotspot" -> true;
            case "production-mix" -> report.dataModel().trafficMix().contains("checkout:")
                    || report.dataModel().trafficMix().contains("hot-sku:")
                    || report.dataModel().trafficMix().contains("flash-sale-hotspot:");
            default -> false;
        };
    }

    private boolean scenarioOutcomePassed(WorkerReport first, CapacityReport.AggregateMetrics metrics,
            List<CapacityReport.StageAggregate> stages) {
        if ("breakpoint".equalsIgnoreCase(first.pattern())) {
            return !stages.isEmpty();
        }
        if ("fault_recovery".equalsIgnoreCase(first.pattern())) {
            return stages.stream().filter(stage -> "recovery".equals(stage.name())).findFirst()
                    .map(stage -> stage.errorRate() <= first.thresholds().maxErrorRate()
                            && stage.p95Micros() <= first.thresholds().maxP95Micros())
                    .orElse(false);
        }
        return metrics.errorRate() <= first.thresholds().maxErrorRate()
                && metrics.p95Micros() <= first.thresholds().maxP95Micros();
    }

    private CapacityReport.CapacityModel capacityModel(WorkerReport first, CapacityReport.AggregateMetrics metrics,
            boolean complete, boolean generatorBottleneck) {
        boolean usableBaseline =
                complete && !generatorBottleneck && metrics.errorRate() <= first.thresholds().maxErrorRate()
                        && metrics.p95Micros() <= first.thresholds().maxP95Micros();
        double safeSingleCellQps =
                usableBaseline ? metrics.measuredQps() * first.capacityInputs().capacityHeadroom() : 0.0;
        double projectedQps =
                safeSingleCellQps * first.capacityInputs().cellCount() * first.capacityInputs().scalingEfficiency();
        double projectedConcurrency = projectedQps * first.capacityInputs().sessionThinkSeconds();
        return new CapacityReport.CapacityModel(metrics.measuredQps(), safeSingleCellQps,
                first.capacityInputs().cellCount(), first.capacityInputs().scalingEfficiency(),
                first.capacityInputs().capacityHeadroom(), projectedQps, first.capacityInputs().sessionThinkSeconds(),
                first.capacityInputs().targetPeakConcurrency(), projectedConcurrency,
                projectedConcurrency >= first.capacityInputs().targetPeakConcurrency(),
                "safeCellQps = measuredQps * headroom; projectedConcurrency = safeCellQps * cells * "
                        + "scalingEfficiency * sessionThinkSeconds");
    }

    private String status(boolean complete, CapacityReport.GeneratorSummary generator, List<String> reasons) {
        if (!complete || generator.bottleneck()) {
            return "INVALID";
        }
        return reasons.isEmpty() ? "PREPRODUCTION_RUN_ELIGIBLE" : "BASELINE_ONLY";
    }

    private Histogram mergeHistograms(List<String> encodedHistograms) {
        Histogram merged = null;
        for (String encoded : encodedHistograms) {
            Histogram histogram = HistogramCodec.decode(encoded);
            if (merged == null) {
                merged = histogram.copy();
            } else {
                merged.add(histogram);
            }
        }
        return merged == null ? new Histogram(3) : merged;
    }

    private long sum(List<WorkerReport> workers, Function<WorkerReport, Long> value) {
        return workers.stream().map(value).mapToLong(Long::longValue).sum();
    }
}
