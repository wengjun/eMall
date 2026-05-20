package com.emall.common.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class BusinessMetrics {
    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, AtomicLong> gauges = new ConcurrentHashMap<>();

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    public void increment(String name, String... tags) {
        meterRegistry.counter(name, Tags.of(tags)).increment();
    }

    public void recordGauge(String name, long value, String... tags) {
        String key = name + Tags.of(tags);
        AtomicLong gauge = gauges.computeIfAbsent(key, ignored -> {
            AtomicLong created = new AtomicLong();
            Gauge.builder(name, created, AtomicLong::get).tags(tags).register(meterRegistry);
            return created;
        });
        gauge.set(value);
    }

    public static BusinessMetrics noop() {
        return new NoopBusinessMetrics();
    }

    private static final class NoopBusinessMetrics extends BusinessMetrics {
        private NoopBusinessMetrics() {
            super(io.micrometer.core.instrument.Metrics.globalRegistry);
        }

        @Override
        public void increment(String name, String... tags) {
        }

        @Override
        public void recordGauge(String name, long value, String... tags) {
        }
    }
}
