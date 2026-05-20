package com.emall.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class BusinessMetricsTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BusinessMetrics metrics = new BusinessMetrics(registry);

    @Test
    void shouldIncrementTaggedCounter() {
        metrics.increment(BusinessMetricNames.ORDER_CREATED, "service", "order", "region", "cn-east");
        metrics.increment(BusinessMetricNames.ORDER_CREATED, "service", "order", "region", "cn-east");

        assertThat(registry.get(BusinessMetricNames.ORDER_CREATED).tags("service", "order", "region", "cn-east")
                .counter().count()).isEqualTo(2.0);
    }

    @Test
    void shouldUpdateExistingGaugeForSameTags() {
        metrics.recordGauge("emall_test_gauge", 3, "service", "search");
        metrics.recordGauge("emall_test_gauge", 7, "service", "search");

        assertThat(registry.get("emall_test_gauge").tags("service", "search").gauge().value()).isEqualTo(7.0);
        assertThat(registry.find("emall_test_gauge").gauges()).hasSize(1);
    }

    @Test
    void noopMetricsShouldIgnoreWrites() {
        BusinessMetrics noop = BusinessMetrics.noop();

        noop.increment(BusinessMetricNames.OUTBOX_FAILED, "service", "order");
        noop.recordGauge("emall_noop_gauge", 9, "service", "order");

        assertThat(registry.find(BusinessMetricNames.OUTBOX_FAILED).counter()).isNull();
    }
}
