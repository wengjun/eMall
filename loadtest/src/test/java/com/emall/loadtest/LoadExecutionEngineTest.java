package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LoadExecutionEngineTest {
    @Test
    void shouldBoundInflightRequestsAndRejectExcessWorkWithoutRetainingFutures() throws Exception {
        Map<String, String> environment = LoadTestOptionsTest.environment();
        environment.put("EMALL_LOAD_RATE", "1000");
        environment.put("EMALL_LOAD_DURATION_MS", "150");
        environment.put("EMALL_LOAD_MAX_INFLIGHT", "2");
        environment.put("EMALL_LOAD_BACKPRESSURE_TIMEOUT_MS", "1");
        environment.put("EMALL_LOAD_MAX_SCHEDULER_LAG_MS", "10000");
        LoadTestOptions options = LoadTestOptions.from(new String[0], environment);
        AtomicInteger inflight = new AtomicInteger();
        AtomicInteger peakInflight = new AtomicInteger();
        RequestDispatcher dispatcher = (sequence, stage) -> CompletableFuture.supplyAsync(() -> {
            int current = inflight.incrementAndGet();
            peakInflight.accumulateAndGet(current, Math::max);
            try {
                TimeUnit.MILLISECONDS.sleep(25L);
                return new RequestResult(true, 25_000L, 200, "");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return RequestResult.failed(25_000L, "interrupted");
            } finally {
                inflight.decrementAndGet();
            }
        });

        WorkerReport report = new LoadExecutionEngine(options, dispatcher).execute();

        assertThat(peakInflight).hasValueLessThanOrEqualTo(2);
        assertThat(report.metrics().peakInflight()).isLessThanOrEqualTo(2);
        assertThat(report.metrics().backpressureRejected()).isPositive();
        assertThat(report.metrics().attempted())
                .isEqualTo(report.metrics().completed() + report.metrics().backpressureRejected());
    }
}
