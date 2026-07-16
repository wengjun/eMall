package com.emall.loadtest;

import java.util.List;
import java.util.Map;

public record WorkerReport(int schemaVersion, String runId, int workerIndex, int workerCount, String role,
        String scenario, String pattern, String startedAt, String finishedAt, long durationMillis, int globalTargetQps,
        int localTargetQps, int maxInflight, RequestMetrics metrics, String histogram, List<StageReport> stages,
        GeneratorMetrics generator, RunMetadata metadata, DataModel dataModel, Map<String, Double> saturationMetrics,
        Thresholds thresholds, CapacityInputs capacityInputs, String faultExperiment) {

    public record RequestMetrics(long attempted, long completed, long success, long failed, long backpressureRejected,
            double errorRate, double measuredQps, long p50Micros, long p95Micros, long p99Micros, long maxMicros,
            int peakInflight, long maxSchedulerLagMicros, long status2xx, long status4xx, long status429,
            long status5xx, long transportErrors) {
    }

    public record StageReport(String name, int targetQps, boolean faultWindow, boolean checkpoint, long completed,
            long success, long failed, String histogram, long p50Micros, long p95Micros, long p99Micros) {
    }

    public record GeneratorMetrics(double processCpuUtilization, long heapUsedBytes, long heapMaxBytes, int peakThreads,
            int availableProcessors, long gcPauseMillis, boolean bottleneck, List<String> reasons) {
    }

    public record RunMetadata(String environment, String evidenceScope, String gitCommit, String deployment,
            String targetResources, int serviceInstances, String authenticationModel) {
    }

    public record DataModel(long datasetUsers, long datasetSkus, int activeUserCardinality, int activeSkuCardinality,
            int hotSkuPercent, String trafficMix) {
    }

    public record Thresholds(double maxErrorRate, long maxP95Micros, long maxSchedulerLagMicros,
            double maxGeneratorCpu) {
    }

    public record CapacityInputs(int cellCount, double scalingEfficiency, double capacityHeadroom,
            long targetPeakConcurrency, double sessionThinkSeconds) {
    }
}
