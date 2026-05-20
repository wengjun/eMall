package com.emall.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TwoLevelCacheTest {
    @Test
    void shouldPromoteRemoteHitIntoLocalCache() {
        ExpiringMapCacheStore<String, String> local = new ExpiringMapCacheStore<>(Duration.ofSeconds(10));
        ExpiringMapCacheStore<String, String> remote = new ExpiringMapCacheStore<>(Duration.ofSeconds(60));
        TwoLevelCache<String, String> cache = new TwoLevelCache<>(local, remote);

        remote.put("sku:1", "product-1");

        assertThat(cache.getIfPresent("sku:1")).contains("product-1");
        assertThat(local.get("sku:1")).contains("product-1");
    }

    @Test
    void shouldLoadMissingValueOnceAndWriteBothLevels() {
        ExpiringMapCacheStore<String, String> local = new ExpiringMapCacheStore<>(Duration.ofSeconds(10));
        ExpiringMapCacheStore<String, String> remote = new ExpiringMapCacheStore<>(Duration.ofSeconds(60));
        TwoLevelCache<String, String> cache = new TwoLevelCache<>(local, remote);
        AtomicInteger loads = new AtomicInteger();

        String first = cache.get("price:1", () -> {
            loads.incrementAndGet();
            return "100.00";
        });
        String second = cache.get("price:1", () -> {
            loads.incrementAndGet();
            return "101.00";
        });

        assertThat(first).isEqualTo("100.00");
        assertThat(second).isEqualTo("100.00");
        assertThat(loads).hasValue(1);
        assertThat(local.get("price:1")).contains("100.00");
        assertThat(remote.get("price:1")).contains("100.00");
    }

    @Test
    void shouldExpireEntriesByTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-12T00:00:00Z"));
        ExpiringMapCacheStore<String, String> store = new ExpiringMapCacheStore<>(Duration.ofSeconds(5), clock);

        store.put("key", "value");
        clock.advance(Duration.ofSeconds(6));

        assertThat(store.get("key")).isEmpty();
    }

    @Test
    void shouldCoalesceConcurrentLoadsForSameKey() throws Exception {
        ExpiringMapCacheStore<String, String> local = new ExpiringMapCacheStore<>(Duration.ofSeconds(10));
        ExpiringMapCacheStore<String, String> remote = new ExpiringMapCacheStore<>(Duration.ofSeconds(60));
        TwoLevelCache<String, String> cache = new TwoLevelCache<>(local, remote);
        AtomicInteger loads = new AtomicInteger();
        List<Callable<String>> tasks = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            tasks.add(() -> cache.get("hot-product", () -> {
                loads.incrementAndGet();
                sleep(Duration.ofMillis(50));
                return "product";
            }));
        }

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<String> values = executor.invokeAll(tasks).stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }
            }).toList();

            assertThat(values).containsOnly("product");
            assertThat(loads).hasValue(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldAddStableTtlJitterByKey() {
        CacheTtlPolicy policy = new CacheTtlPolicy(Duration.ofSeconds(100), 0.1);

        assertThat(policy.ttlForKey("sku:1")).isBetween(Duration.ofSeconds(100), Duration.ofSeconds(110));
        assertThat(policy.ttlForKey("sku:1")).isEqualTo(policy.ttlForKey("sku:1"));
    }

    @Test
    void shouldRejectInvalidTtlPolicyArguments() {
        assertThatThrownBy(() -> new CacheTtlPolicy(Duration.ZERO, 0.1)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseTtl");
        assertThatThrownBy(() -> new CacheTtlPolicy(Duration.ofSeconds(1), 1.1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("jitterRatio");
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
