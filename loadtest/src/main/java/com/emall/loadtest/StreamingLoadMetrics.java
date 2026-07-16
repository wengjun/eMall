package com.emall.loadtest;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

final class StreamingLoadMetrics {
    private static final long HIGHEST_TRACKABLE_MICROS = 3_600_000_000L;
    private static final int SIGNIFICANT_DIGITS = 3;

    private final Recorder latencyRecorder = new Recorder(HIGHEST_TRACKABLE_MICROS, SIGNIFICANT_DIGITS);
    private final Histogram cumulativeLatency = new Histogram(HIGHEST_TRACKABLE_MICROS, SIGNIFICANT_DIGITS);
    private final Map<String, StageMetrics> stages = new ConcurrentHashMap<>();
    private final LongAdder attempted = new LongAdder();
    private final LongAdder completed = new LongAdder();
    private final LongAdder success = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder backpressureRejected = new LongAdder();
    private final LongAdder status2xx = new LongAdder();
    private final LongAdder status4xx = new LongAdder();
    private final LongAdder status429 = new LongAdder();
    private final LongAdder status5xx = new LongAdder();
    private final LongAdder transportErrors = new LongAdder();
    private final AtomicInteger inflight = new AtomicInteger();
    private final AtomicInteger peakInflight = new AtomicInteger();
    private final AtomicLong maxSchedulerLagMicros = new AtomicLong();

    void recordAttempt() {
        attempted.increment();
    }

    void recordBackpressureRejection() {
        backpressureRejected.increment();
    }

    void requestStarted() {
        int current = inflight.incrementAndGet();
        peakInflight.accumulateAndGet(current, Math::max);
    }

    void recordCompletion(String stageName, RequestResult result) {
        long latency = Math.max(1L, Math.min(HIGHEST_TRACKABLE_MICROS, result.latencyMicros()));
        latencyRecorder.recordValue(latency);
        completed.increment();
        inflight.decrementAndGet();
        if (result.success()) {
            success.increment();
        } else {
            failed.increment();
        }
        recordStatus(result.statusCode());
        stages.computeIfAbsent(stageName, ignored -> new StageMetrics()).record(result, latency);
    }

    void recordSchedulerLag(long lagMicros) {
        maxSchedulerLagMicros.accumulateAndGet(Math.max(0L, lagMicros), Math::max);
    }

    synchronized Snapshot snapshot() {
        cumulativeLatency.add(latencyRecorder.getIntervalHistogram());
        Histogram histogram = cumulativeLatency.copy();
        List<StageSnapshot> stageSnapshots =
                stages.entrySet().stream().map(entry -> entry.getValue().snapshot(entry.getKey()))
                        .sorted(Comparator.comparing(StageSnapshot::name)).toList();
        return new Snapshot(attempted.sum(), completed.sum(), success.sum(), failed.sum(), backpressureRejected.sum(),
                status2xx.sum(), status4xx.sum(), status429.sum(), status5xx.sum(), transportErrors.sum(),
                peakInflight.get(), maxSchedulerLagMicros.get(), histogram, stageSnapshots);
    }

    private void recordStatus(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            status2xx.increment();
        } else if (statusCode == 429) {
            status4xx.increment();
            status429.increment();
        } else if (statusCode >= 400 && statusCode < 500) {
            status4xx.increment();
        } else if (statusCode >= 500) {
            status5xx.increment();
        } else {
            transportErrors.increment();
        }
    }

    record Snapshot(long attempted, long completed, long success, long failed, long backpressureRejected,
            long status2xx, long status4xx, long status429, long status5xx, long transportErrors, int peakInflight,
            long maxSchedulerLagMicros, Histogram histogram, List<StageSnapshot> stages) {
        double errorRate() {
            long errors = failed + backpressureRejected;
            return attempted == 0L ? 0.0 : (double) errors / attempted;
        }

        long percentile(double percentile) {
            return histogram.getValueAtPercentile(percentile);
        }
    }

    record StageSnapshot(String name, long completed, long success, long failed, String histogram, long p50Micros,
            long p95Micros, long p99Micros) {
    }

    private static final class StageMetrics {
        private final Recorder recorder = new Recorder(HIGHEST_TRACKABLE_MICROS, SIGNIFICANT_DIGITS);
        private final Histogram cumulative = new Histogram(HIGHEST_TRACKABLE_MICROS, SIGNIFICANT_DIGITS);
        private final LongAdder completed = new LongAdder();
        private final LongAdder success = new LongAdder();
        private final LongAdder failed = new LongAdder();

        void record(RequestResult result, long latency) {
            recorder.recordValue(latency);
            completed.increment();
            if (result.success()) {
                success.increment();
            } else {
                failed.increment();
            }
        }

        synchronized StageSnapshot snapshot(String name) {
            cumulative.add(recorder.getIntervalHistogram());
            Histogram copy = cumulative.copy();
            return new StageSnapshot(name, completed.sum(), success.sum(), failed.sum(), HistogramCodec.encode(copy),
                    copy.getValueAtPercentile(50.0), copy.getValueAtPercentile(95.0), copy.getValueAtPercentile(99.0));
        }
    }
}
