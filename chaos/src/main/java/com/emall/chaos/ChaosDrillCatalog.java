package com.emall.chaos;

import java.time.Duration;
import java.util.List;

public final class ChaosDrillCatalog {
    private ChaosDrillCatalog() {
    }

    public static List<ChaosDrill> p3Baseline() {
        return List.of(
                redisFailure(),
                mqBacklog(),
                databaseFailover(),
                downstreamLatency(),
                partialRegionIsolation()
        );
    }

    public static ChaosDrill redisFailure() {
        return new ChaosDrill("redis-failure", DrillType.REDIS_FAILURE, "redis", BlastRadius.SINGLE_SERVICE,
                Duration.ofMinutes(10), standardPrerequisites(),
                List.of("pause redis pod network for cache clients", "verify pricing/product cache fallback"),
                standardAbortConditions(),
                List.of("redis ready probe is healthy", "cache hit ratio returns above 80 percent"));
    }

    public static ChaosDrill mqBacklog() {
        return new ChaosDrill("mq-backlog", DrillType.MQ_BACKLOG, "kafka", BlastRadius.SINGLE_SERVICE,
                Duration.ofMinutes(15), standardPrerequisites(),
                List.of("throttle fulfillment consumer group", "generate product and order event backlog"),
                standardAbortConditions(),
                List.of("consumer lag drains below 1000", "dead-letter topic growth stops"));
    }

    public static ChaosDrill databaseFailover() {
        return new ChaosDrill("database-failover", DrillType.DATABASE_FAILOVER, "mysql", BlastRadius.SINGLE_REGION,
                Duration.ofMinutes(20), standardPrerequisites(),
                List.of("promote replica writer in staging", "verify JDBC reconnect and compensation jobs"),
                standardAbortConditions(),
                List.of("write probes pass", "pending compensation backlog returns to baseline"));
    }

    public static ChaosDrill downstreamLatency() {
        return new ChaosDrill("downstream-latency", DrillType.DOWNSTREAM_LATENCY, "inventory",
                BlastRadius.SINGLE_SERVICE, Duration.ofMinutes(10), standardPrerequisites(),
                List.of("inject 800ms latency into inventory service", "verify order circuit breaker behavior"),
                standardAbortConditions(),
                List.of("order error ratio returns below 1 percent", "adaptive recovery returns to open traffic"));
    }

    public static ChaosDrill partialRegionIsolation() {
        return new ChaosDrill("partial-region-isolation", DrillType.PARTIAL_REGION_ISOLATION, "us-west-2",
                BlastRadius.SINGLE_REGION, Duration.ofMinutes(20), standardPrerequisites(),
                List.of("drop east-to-west service traffic", "verify local reads and owner-region write blocking"),
                standardAbortConditions(),
                List.of("multi-region routing returns expected owner", "cross-region error budget burn normalizes"));
    }

    private static List<String> standardPrerequisites() {
        return List.of("rollback owner assigned", "SLO dashboard open", "on-call engineer acknowledged");
    }

    private static List<AbortCondition> standardAbortConditions() {
        return List.of(
                new AbortCondition("http_server_error_ratio", ">", 5.0, 2),
                new AbortCondition("checkout_p99_latency_ms", ">", 2000.0, 2),
                new AbortCondition("payment_error_ratio", ">", 1.0, 1)
        );
    }
}
