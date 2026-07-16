package com.emall.common.sharding;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;

final class ShardConnectionPoolMetrics {
    private ShardConnectionPoolMetrics() {
    }

    static void bind(MeterRegistry registry, List<LazyShardDataSource> dataSources,
            Map<String, ShardConnectionBudget.PoolAllocation> allocations) {
        if (registry == null) {
            return;
        }
        int maximumConnections =
                allocations.values().stream().mapToInt(ShardConnectionBudget.PoolAllocation::maximumPoolSize).sum();
        Gauge.builder("emall_db_connections_active", dataSources, ShardConnectionPoolMetrics::activeConnections)
                .description("Active connections across initialized local shard pools").register(registry);
        Gauge.builder("emall_db_connection_utilization_ratio", dataSources,
                sources -> maximumConnections == 0 ? 0 : activeConnections(sources) / maximumConnections)
                .description("Active shard connections divided by the pod connection budget").register(registry);
        Gauge.builder("emall_db_shard_pools_initialized", dataSources,
                sources -> sources.stream().filter(source -> source.initializedDataSource().isPresent()).count())
                .description("Number of initialized shard connection pools").register(registry);
    }

    private static double activeConnections(List<LazyShardDataSource> dataSources) {
        return dataSources.stream().map(LazyShardDataSource::initializedDataSource).flatMap(java.util.Optional::stream)
                .filter(HikariDataSource.class::isInstance).map(HikariDataSource.class::cast)
                .filter(dataSource -> dataSource.getHikariPoolMXBean() != null)
                .mapToInt(dataSource -> dataSource.getHikariPoolMXBean().getActiveConnections()).sum();
    }
}
