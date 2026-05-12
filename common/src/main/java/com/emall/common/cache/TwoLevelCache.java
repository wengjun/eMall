package com.emall.common.cache;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class TwoLevelCache<K, V> {
    private final CacheStore<K, V> localCache;
    private final CacheStore<K, V> remoteCache;

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
        return getIfPresent(key).orElseGet(() -> {
            V loaded = Objects.requireNonNull(loader.get(), "loaded value must not be null");
            put(key, loaded);
            return loaded;
        });
    }

    public void put(K key, V value) {
        remoteCache.put(key, value);
        localCache.put(key, value);
    }

    public void evict(K key) {
        localCache.evict(key);
        remoteCache.evict(key);
    }

    public interface CacheStore<K, V> {
        Optional<V> get(K key);

        void put(K key, V value);

        void evict(K key);
    }
}
