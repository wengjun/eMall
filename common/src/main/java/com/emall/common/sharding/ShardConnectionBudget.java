package com.emall.common.sharding;

import java.util.LinkedHashMap;
import java.util.Map;

final class ShardConnectionBudget {
    private ShardConnectionBudget() {
    }

    static Map<String, PoolAllocation> plan(ShardDataSourceProperties properties,
            Map<String, ShardDataSourceProperties.DataSourceSpec> datasources) {
        validateGlobalProperties(properties);
        Map<String, PoolAllocation> allocations = new LinkedHashMap<>();
        long podMaximum = 0;
        for (var entry : datasources.entrySet()) {
            var spec = entry.getValue();
            int maximum = spec.getMaximumPoolSize() == null
                    ? properties.getDefaultMaximumPoolSize()
                    : spec.getMaximumPoolSize();
            int minimum = spec.getMinimumIdle() == null ? properties.getDefaultMinimumIdle() : spec.getMinimumIdle();
            if (maximum <= 0 || minimum < 0 || minimum > maximum) {
                throw new IllegalStateException("invalid shard pool size for " + entry.getKey());
            }
            if ((long) maximum * properties.getPlannedMaxReplicas() > effectiveBudget(
                    properties.getDatabaseInstanceConnectionBudget(), properties.getConnectionHeadroomPercent())) {
                throw new IllegalStateException("database instance connection budget exceeded by " + entry.getKey());
            }
            allocations.put(entry.getKey(), new PoolAllocation(maximum, minimum));
            podMaximum += maximum;
        }
        if (podMaximum > properties.getPodConnectionBudget()) {
            throw new IllegalStateException(
                    "pod connection budget exceeded: " + podMaximum + " > " + properties.getPodConnectionBudget());
        }
        long plannedGlobal = podMaximum * properties.getPlannedMaxReplicas();
        long effectiveGlobal =
                effectiveBudget(properties.getGlobalConnectionBudget(), properties.getConnectionHeadroomPercent());
        if (plannedGlobal > effectiveGlobal) {
            throw new IllegalStateException(
                    "global connection budget exceeded: " + plannedGlobal + " > " + effectiveGlobal);
        }
        return Map.copyOf(allocations);
    }

    private static void validateGlobalProperties(ShardDataSourceProperties properties) {
        if (properties.getDefaultMaximumPoolSize() <= 0 || properties.getDefaultMinimumIdle() < 0
                || properties.getDefaultMinimumIdle() > properties.getDefaultMaximumPoolSize()) {
            throw new IllegalStateException("invalid default shard pool size");
        }
        if (properties.getPodConnectionBudget() <= 0 || properties.getPlannedMaxReplicas() <= 0
                || properties.getDatabaseInstanceConnectionBudget() <= 0
                || properties.getGlobalConnectionBudget() <= 0) {
            throw new IllegalStateException("connection budgets and planned replicas must be positive");
        }
        if (properties.getConnectionHeadroomPercent() < 0 || properties.getConnectionHeadroomPercent() >= 100) {
            throw new IllegalStateException("connection headroom percent must be between 0 and 99");
        }
        if (properties.getIdleTimeoutMs() < 10000 || properties.getMaxLifetimeMs() < 30000) {
            throw new IllegalStateException("invalid shard pool lifecycle timeout");
        }
    }

    private static long effectiveBudget(int budget, int headroomPercent) {
        return (long) budget * (100 - headroomPercent) / 100;
    }

    record PoolAllocation(int maximumPoolSize, int minimumIdle) {
    }
}
