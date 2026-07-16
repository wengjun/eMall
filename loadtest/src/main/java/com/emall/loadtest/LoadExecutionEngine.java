package com.emall.loadtest;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

final class LoadExecutionEngine {
    private final LoadTestOptions options;
    private final RequestDispatcher dispatcher;
    private final StreamingLoadMetrics metrics;

    LoadExecutionEngine(LoadTestOptions options, RequestDispatcher dispatcher) {
        this(options, dispatcher, new StreamingLoadMetrics());
    }

    LoadExecutionEngine(LoadTestOptions options, RequestDispatcher dispatcher, StreamingLoadMetrics metrics) {
        this.options = options;
        this.dispatcher = dispatcher;
        this.metrics = metrics;
    }

    WorkerReport execute() throws InterruptedException {
        Semaphore inflightPermits = new Semaphore(options.maxInflight());
        Phaser completions = new Phaser(1);
        SystemResourceSampler resourceSampler = new SystemResourceSampler();
        Instant startedAt = Instant.now();
        long startedNanos = System.nanoTime();
        long endNanos = startedNanos + options.duration().toNanos();
        long nextDispatchNanos = startedNanos;
        long localSequence = 0L;
        LoadPattern.StageDefinition previousStage = null;

        while (System.nanoTime() < endNanos) {
            long now = System.nanoTime();
            LoadPattern.StageDefinition stage = options.pattern().stageAt(Duration.ofNanos(now - startedNanos),
                    options.duration(), options.ratePerSecond());
            if (previousStage == null || !previousStage.name().equals(stage.name())) {
                if (previousStage != null && shouldStopAtBreakpoint(previousStage)) {
                    break;
                }
                previousStage = stage;
                nextDispatchNanos = now;
            }

            int localRate = options.localRate(stage.globalRate());
            if (localRate == 0) {
                TimeUnit.MILLISECONDS.sleep(10L);
                continue;
            }
            long intervalNanos = Math.max(1L, TimeUnit.SECONDS.toNanos(1L) / localRate);
            sleepUntil(nextDispatchNanos);
            now = System.nanoTime();
            metrics.recordSchedulerLag(TimeUnit.NANOSECONDS.toMicros(Math.max(0L, now - nextDispatchNanos)));
            metrics.recordAttempt();

            if (!inflightPermits.tryAcquire(options.backpressureTimeout().toNanos(), TimeUnit.NANOSECONDS)) {
                metrics.recordBackpressureRejection();
                nextDispatchNanos = Math.max(nextDispatchNanos + intervalNanos, System.nanoTime());
                continue;
            }

            localSequence++;
            long globalSequence = options.worker().globalSequence(localSequence);
            submit(globalSequence, stage, inflightPermits, completions);
            nextDispatchNanos = Math.max(nextDispatchNanos + intervalNanos, System.nanoTime());
        }

        completions.arriveAndAwaitAdvance();
        Instant finishedAt = Instant.now();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedNanos);
        return WorkerReportFactory.create(options, metrics.snapshot(), resourceSampler.finish(), startedAt, finishedAt,
                elapsed);
    }

    private void submit(long globalSequence, LoadPattern.StageDefinition stage, Semaphore inflightPermits,
            Phaser completions) {
        long startedNanos = System.nanoTime();
        completions.register();
        metrics.requestStarted();
        try {
            CompletionStage<RequestResult> request = dispatcher.dispatch(globalSequence, stage);
            if (request == null) {
                complete(stage.name(), RequestResult.failed(elapsedMicros(startedNanos), "null-completion-stage"),
                        inflightPermits, completions);
                return;
            }
            request.whenComplete((result, error) -> {
                RequestResult completed = error == null && result != null
                        ? result
                        : RequestResult.failed(elapsedMicros(startedNanos), errorKind(error));
                complete(stage.name(), completed, inflightPermits, completions);
            });
        } catch (RuntimeException ex) {
            complete(stage.name(), RequestResult.failed(elapsedMicros(startedNanos), errorKind(ex)), inflightPermits,
                    completions);
        }
    }

    private void complete(String stageName, RequestResult result, Semaphore inflightPermits, Phaser completions) {
        try {
            metrics.recordCompletion(stageName, result);
        } finally {
            inflightPermits.release();
            completions.arriveAndDeregister();
        }
    }

    private boolean shouldStopAtBreakpoint(LoadPattern.StageDefinition completedStage) {
        if (options.pattern() != LoadPattern.BREAKPOINT || !completedStage.checkpoint()) {
            return false;
        }
        StreamingLoadMetrics.Snapshot checkpoint = metrics.snapshot();
        return checkpoint.errorRate() > options.maxErrorRate() || checkpoint.percentile(95.0) > options.maxP95Micros();
    }

    private void sleepUntil(long targetNanos) throws InterruptedException {
        long remaining = targetNanos - System.nanoTime();
        if (remaining > 0L) {
            TimeUnit.NANOSECONDS.sleep(remaining);
        }
    }

    private long elapsedMicros(long startedNanos) {
        return Math.max(1L, TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startedNanos));
    }

    private String errorKind(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName();
    }
}
