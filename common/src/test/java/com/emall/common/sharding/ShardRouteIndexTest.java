package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ShardRouteIndexTest {
    private final ShardRouteIndex routeIndex = ShardRouteIndex.local();

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void removesPendingUniqueRouteWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        routeIndex.bindUniqueTransactional("order-id", "1001", 2001L);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();

        synchronizations.forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(routeIndex.resolve("order-id", "1001")).isEmpty();
    }

    @Test
    void removesReleasedRouteBeforeCommitAndRestoresItOnRollback() {
        routeIndex.bindUnique("coupon", "coupon-1", 3001L);
        TransactionSynchronizationManager.initSynchronization();
        routeIndex.removeIfOwnedTransactional("coupon", "coupon-1", 3001L);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();

        assertThat(routeIndex.resolve("coupon", "coupon-1")).hasValue(3001L);
        synchronizations.forEach(synchronization -> synchronization.beforeCommit(false));
        assertThat(routeIndex.resolve("coupon", "coupon-1")).isEmpty();
        synchronizations.forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(routeIndex.resolve("coupon", "coupon-1")).hasValue(3001L);
    }

    @Test
    void resolvesFromPersistentDirectoryWhenRedisIsUnavailable() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(any())).thenThrow(new IllegalStateException("redis unavailable"));
        InMemoryShardRouteDirectory directory = new InMemoryShardRouteDirectory();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ShardRouteIndex index = new ShardRouteIndex(redis, true, directory, new ShardRouteDirectoryProperties(),
                java.time.Clock.systemUTC(), new BusinessMetrics(meterRegistry));

        index.bindUnique("order-id", "1001", 2001L);

        assertThat(index.resolve("order-id", "1001")).hasValue(2001L);
        assertThat(meterRegistry.counter(BusinessMetricNames.SHARD_ROUTE_CACHE_MISS, "reason", "redis-error").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.counter(BusinessMetricNames.SHARD_ROUTE_DIRECTORY_LOOKUP, "result", "found").count())
                .isEqualTo(1.0);
    }

    @Test
    void appliesFiniteRetentionToHighCardinalityRequestRoutes() {
        java.time.Clock clock =
                java.time.Clock.fixed(java.time.Instant.parse("2026-07-15T00:00:00Z"), java.time.ZoneOffset.UTC);
        InMemoryShardRouteDirectory directory = new InMemoryShardRouteDirectory(clock);
        ShardRouteIndex index = new ShardRouteIndex(null, true, directory, new ShardRouteDirectoryProperties(), clock);

        index.bindUnique("order-request", "request-1", 2001L);

        ShardRouteRecord route = directory.scan(null, 10).routes().get(0);
        assertThat(Duration.between(route.createdAt(), route.expiresAt())).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void rebuildsRedisCacheFromTheAuthoritativeDirectory() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        InMemoryShardRouteDirectory directory = new InMemoryShardRouteDirectory();
        ShardRouteIndex index = new ShardRouteIndex(redis, true, directory, new ShardRouteDirectoryProperties(),
                java.time.Clock.systemUTC());
        index.bindUnique("order-id", "1001", 2001L);
        index.bindUnique("order-id", "1002", 2002L);

        assertThat(index.rebuildCache()).isEqualTo(2);
    }

    @Test
    void rebuildsLargeRouteDirectoriesInBoundedCursorPages() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        InMemoryShardRouteDirectory directory = new InMemoryShardRouteDirectory();
        ShardRouteIndex index = new ShardRouteIndex(redis, true, directory, new ShardRouteDirectoryProperties(),
                java.time.Clock.systemUTC());
        index.bindUnique("order-id", "1001", 2001L);
        index.bindUnique("order-id", "1002", 2002L);
        index.bindUnique("order-id", "1003", 2003L);

        ShardRouteCacheRebuildResult first = index.rebuildCache(null, 2);
        ShardRouteCacheRebuildResult second = index.rebuildCache(first.nextCursor(), 2);

        assertThat(first.rebuilt()).isEqualTo(2);
        assertThat(first.complete()).isFalse();
        assertThat(second.rebuilt()).isEqualTo(1);
        assertThat(second.complete()).isTrue();
    }
}
