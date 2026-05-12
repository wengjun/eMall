package com.emall.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
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
