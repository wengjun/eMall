package com.emall.loadtest;

import java.util.List;
import java.util.Map;

public record CapacityReport(int schemaVersion, String runId, String status, List<String> reasons, String scenario,
        String pattern, String startedAt, String finishedAt, int expectedWorkers, int receivedWorkers,
        boolean workerSetComplete, int globalTargetQps, int maxInflightPerWorker, AggregateMetrics metrics,
        String histogram, List<StageAggregate> stages, GeneratorSummary generator, WorkerReport.RunMetadata metadata,
        WorkerReport.DataModel dataModel, Map<String, Double> saturationMetrics, WorkerReport.Thresholds thresholds,
        CapacityModel capacityModel, String faultExperiment) {

    public record AggregateMetrics(long attempted, long completed, long success, long failed, long backpressureRejected,
            double errorRate, double measuredQps, long p50Micros, long p95Micros, long p99Micros, long maxMicros,
            int peakInflight, long maxSchedulerLagMicros, long status2xx, long status4xx, long status429,
            long status5xx, long transportErrors) {
    }

    public record StageAggregate(String name, int targetQps, boolean faultWindow, boolean checkpoint, long completed,
            long success, long failed, double errorRate, long p50Micros, long p95Micros, long p99Micros) {
    }

    public record GeneratorSummary(double maxProcessCpuUtilization, long totalHeapUsedBytes, long totalHeapMaxBytes,
            int totalPeakThreads, int totalAvailableProcessors, long maxGcPauseMillis, boolean bottleneck,
            List<String> reasons) {
    }

    public record CapacityModel(double measuredSingleCellQps, double safeSingleCellQps, int projectedCells,
            double scalingEfficiency, double headroom, double projectedQps, double sessionThinkSeconds,
            long targetPeakConcurrency, double projectedPeakConcurrency, boolean targetMet, String formula) {
    }
}
