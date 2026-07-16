package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import org.HdrHistogram.Histogram;
import org.junit.jupiter.api.Test;

class StreamingLoadMetricsTest {
    @Test
    void shouldStreamLargeResultSetsIntoMergeableHistogram() {
        StreamingLoadMetrics metrics = new StreamingLoadMetrics();

        for (int index = 1; index <= 100_000; index++) {
            metrics.recordAttempt();
            metrics.requestStarted();
            metrics.recordCompletion("steady",
                    new RequestResult(index % 10 != 0, index, index % 10 == 0 ? 500 : 200, ""));
        }
        StreamingLoadMetrics.Snapshot first = metrics.snapshot();
        StreamingLoadMetrics.Snapshot second = metrics.snapshot();
        Histogram decoded = HistogramCodec.decode(HistogramCodec.encode(second.histogram()));

        assertThat(first.attempted()).isEqualTo(100_000L);
        assertThat(first.success()).isEqualTo(90_000L);
        assertThat(first.status5xx()).isEqualTo(10_000L);
        assertThat(first.histogram().getTotalCount()).isEqualTo(100_000L);
        assertThat(second.histogram().getTotalCount()).isEqualTo(100_000L);
        assertThat(decoded.getTotalCount()).isEqualTo(100_000L);
        assertThat(decoded.getValueAtPercentile(99.0)).isGreaterThan(98_000L);
    }
}
