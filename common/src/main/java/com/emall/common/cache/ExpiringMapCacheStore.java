package com.emall.common.cache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ExpiringMapCacheStore<K, V> implements TwoLevelCache.CacheStore<K, V> {
    private final ConcurrentMap<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    public ExpiringMapCacheStore(Duration ttl) {
        this(ttl, Clock.systemUTC());
    }

    public ExpiringMapCacheStore(Duration ttl, Clock clock) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.ttl = ttl;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Optional<V> get(K key) {
        Entry<V> entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(Instant.now(clock))) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void put(K key, V value) {
        entries.put(key, new Entry<>(Objects.requireNonNull(value, "value must not be null"),
                Instant.now(clock).plus(ttl)));
    }

    @Override
    public void evict(K key) {
        entries.remove(key);
    }

    private record Entry<V>(V value, Instant expiresAt) {
    }
}
