package com.emall.common.cache;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TwoLevelCache<K, V> {
    private final CacheStore<K, V> localCache;
    private final CacheStore<K, V> remoteCache;
    private final ConcurrentMap<K, CompletableFuture<V>> inFlightLoads = new ConcurrentHashMap<>();

    public TwoLevelCache(CacheStore<K, V> localCache, CacheStore<K, V> remoteCache) {
        this.localCache = Objects.requireNonNull(localCache, "localCache must not be null");
        this.remoteCache = Objects.requireNonNull(remoteCache, "remoteCache must not be null");
    }

    public Optional<V> getIfPresent(K key) {
        return localCache.get(key).or(() -> remoteCache.get(key).map(value -> {
            localCache.put(key, value);
            return value;
        }));
    }

    public V get(K key, Supplier<V> loader) {
        return get(key, loader, ignored -> null);
    }

    public V get(K key, Supplier<V> loader, Function<V, Duration> ttlSelector) {
        return getIfPresent(key).orElseGet(() -> loadOnce(key, loader, ttlSelector));
    }

    public void put(K key, V value) {
        remoteCache.put(key, value);
        localCache.put(key, value);
    }

    public void put(K key, V value, Duration ttl) {
        remoteCache.put(key, value, ttl);
        localCache.put(key, value, ttl);
    }

    public void evict(K key) {
        localCache.evict(key);
        remoteCache.evict(key);
    }

    private V loadOnce(K key, Supplier<V> loader, Function<V, Duration> ttlSelector) {
        CompletableFuture<V> future =
                inFlightLoads.computeIfAbsent(key, ignored -> CompletableFuture.supplyAsync(() -> {
                    V loaded = Objects.requireNonNull(loader.get(), "loaded value must not be null");
                    put(key, loaded, ttlSelector.apply(loaded));
                    return loaded;
                }));
        try {
            return future.join();
        } finally {
            inFlightLoads.remove(key, future);
        }
    }

    public interface CacheStore<K, V> {
        Optional<V> get(K key);

        void put(K key, V value);

        default void put(K key, V value, Duration ttl) {
            put(key, value);
        }

        void evict(K key);
    }
}
