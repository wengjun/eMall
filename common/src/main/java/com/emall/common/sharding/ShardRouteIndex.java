package com.emall.common.sharding;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class ShardRouteIndex {
    private static final String KEY_PREFIX = "emall:shard-route:";
    private static final Duration PENDING_ROUTE_TTL = Duration.ofMinutes(10);
    private static final int MAXIMUM_REBUILD_RECORDS = 100_000;
    private final StringRedisTemplate redisTemplate;
    private final boolean distributedRequired;
    private final ShardRouteDirectory directory;
    private final ShardRouteDirectoryProperties properties;
    private final Clock clock;
    private final BusinessMetrics metrics;

    ShardRouteIndex(StringRedisTemplate redisTemplate, boolean distributedRequired) {
        this(redisTemplate, distributedRequired, new InMemoryShardRouteDirectory(), new ShardRouteDirectoryProperties(),
                Clock.systemUTC(), BusinessMetrics.noop());
    }

    ShardRouteIndex(StringRedisTemplate redisTemplate, boolean distributedRequired, ShardRouteDirectory directory,
            ShardRouteDirectoryProperties properties, Clock clock) {
        this(redisTemplate, distributedRequired, directory, properties, clock, BusinessMetrics.noop());
    }

    ShardRouteIndex(StringRedisTemplate redisTemplate, boolean distributedRequired, ShardRouteDirectory directory,
            ShardRouteDirectoryProperties properties, Clock clock, BusinessMetrics metrics) {
        this.redisTemplate = redisTemplate;
        this.distributedRequired = distributedRequired;
        this.directory = directory;
        this.properties = properties;
        this.clock = clock;
        this.metrics = metrics;
    }

    public static ShardRouteIndex local() {
        return new ShardRouteIndex(null, false);
    }

    public void bind(String namespace, String lookupKey, long shardKey) {
        write(namespace, lookupKey, shardKey, null, false);
    }

    public void bind(String namespace, String lookupKey, long shardKey, Duration ttl) {
        write(namespace, lookupKey, shardKey, ttl, false);
    }

    public void bindUnique(String namespace, String lookupKey, long shardKey) {
        write(namespace, lookupKey, shardKey, null, true);
    }

    public void bindUniqueTransactional(String namespace, String lookupKey, long shardKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            bindUnique(namespace, lookupKey, shardKey);
            return;
        }
        RouteCoordinates coordinates = coordinates(namespace, lookupKey);
        Optional<ShardRouteRecord> previous = directory.resolve(namespace, coordinates.lookupHash());
        AtomicReference<ShardRouteRecord> latest =
                new AtomicReference<>(write(namespace, lookupKey, shardKey, PENDING_ROUTE_TTL, true));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                latest.set(write(namespace, lookupKey, shardKey, null, true));
            }

            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    rollbackBinding(coordinates, latest.get(), previous);
                }
            }
        });
    }

    public OptionalLong resolve(String namespace, String lookupKey) {
        RouteCoordinates coordinates = coordinates(namespace, lookupKey);
        OptionalLong cached = cached(coordinates.cacheKey());
        if (cached.isPresent()) {
            return cached;
        }
        Optional<ShardRouteRecord> route = directory.resolve(namespace, coordinates.lookupHash());
        metrics.increment(BusinessMetricNames.SHARD_ROUTE_DIRECTORY_LOOKUP, "result",
                route.isPresent() ? "found" : "missing");
        if (route.isEmpty() || route.get().expired(clock.instant())) {
            return OptionalLong.empty();
        }
        cache(coordinates.cacheKey(), route.get());
        return OptionalLong.of(route.get().shardKey());
    }

    public long resolveRequired(String namespace, String lookupKey, long localFallback) {
        OptionalLong route = resolve(namespace, lookupKey);
        if (route.isPresent()) {
            return route.getAsLong();
        }
        if (distributedRequired) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "required global shard route does not exist");
        }
        return localFallback;
    }

    public void removeIfOwned(String namespace, String lookupKey, long shardKey) {
        RouteCoordinates coordinates = coordinates(namespace, lookupKey);
        if (directory.removeIfOwned(namespace, coordinates.lookupHash(), shardKey)) {
            evict(coordinates.cacheKey());
        }
    }

    public void removeIfOwnedTransactional(String namespace, String lookupKey, long shardKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            removeIfOwned(namespace, lookupKey, shardKey);
            return;
        }
        RouteCoordinates coordinates = coordinates(namespace, lookupKey);
        Optional<ShardRouteRecord> previous = directory.resolve(namespace, coordinates.lookupHash());
        AtomicReference<Boolean> removed = new AtomicReference<>(false);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                Long expectedVersion = previous.map(ShardRouteRecord::version).orElse(null);
                removed.set(directory.removeIfOwned(namespace, coordinates.lookupHash(), shardKey, expectedVersion));
                if (removed.get()) {
                    evict(coordinates.cacheKey());
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK && removed.get() && previous.isPresent()) {
                    restore(coordinates, previous.get());
                }
            }
        });
    }

    public int rebuildCache() {
        return rebuildCache(null, MAXIMUM_REBUILD_RECORDS).rebuilt();
    }

    public ShardRouteCacheRebuildResult rebuildCache(String startCursor, int requestedLimit) {
        if (redisTemplate == null) {
            return new ShardRouteCacheRebuildResult(0, startCursor, true);
        }
        int limit = Math.max(1, Math.min(requestedLimit, MAXIMUM_REBUILD_RECORDS));
        int rebuilt = 0;
        String cursor = startCursor;
        String nextCursor;
        do {
            ShardRoutePage page = directory.scan(cursor, Math.min(1000, limit - rebuilt));
            for (ShardRouteRecord route : page.routes()) {
                cache(cacheKey(route.namespace(), route.lookupHash()), route);
                rebuilt++;
            }
            nextCursor = page.nextCursor();
            cursor = nextCursor;
        } while (nextCursor != null && rebuilt < limit);
        metrics.recordGauge(BusinessMetricNames.SHARD_ROUTE_CACHE_REBUILT, rebuilt);
        return new ShardRouteCacheRebuildResult(rebuilt, nextCursor, nextCursor == null);
    }

    private ShardRouteRecord write(String namespace, String lookupKey, long shardKey, Duration requestedTtl,
            boolean unique) {
        RouteCoordinates coordinates = coordinates(namespace, lookupKey);
        Duration retention = requestedTtl == null ? properties.retentionFor(namespace) : requestedTtl;
        Instant expiresAt = retention == null ? null : clock.instant().plus(retention);
        ShardRouteRecord route = directory.bind(namespace, coordinates.lookupHash(), shardKey, expiresAt, unique);
        cache(coordinates.cacheKey(), route);
        return route;
    }

    private OptionalLong cached(String key) {
        if (redisTemplate == null) {
            return OptionalLong.empty();
        }
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                metrics.increment(BusinessMetricNames.SHARD_ROUTE_CACHE_MISS, "reason", "absent");
                return OptionalLong.empty();
            }
            metrics.increment(BusinessMetricNames.SHARD_ROUTE_CACHE_HIT);
            return OptionalLong.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            metrics.increment(BusinessMetricNames.SHARD_ROUTE_CACHE_MISS, "reason", "invalid");
            evict(key);
            return OptionalLong.empty();
        } catch (RuntimeException ex) {
            metrics.increment(BusinessMetricNames.SHARD_ROUTE_CACHE_MISS, "reason", "redis-error");
            return OptionalLong.empty();
        }
    }

    private void cache(String key, ShardRouteRecord route) {
        if (redisTemplate == null) {
            return;
        }
        Duration ttl = properties.getCacheTtl();
        if (route.expiresAt() != null) {
            Duration remaining = Duration.between(clock.instant(), route.expiresAt());
            if (remaining.isNegative() || remaining.isZero()) {
                evict(key);
                return;
            }
            if (ttl == null || remaining.compareTo(ttl) < 0) {
                ttl = remaining;
            }
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalStateException("route cache TTL must be positive");
        }
        try {
            redisTemplate.opsForValue().set(key, Long.toString(route.shardKey()), ttl);
        } catch (RuntimeException ignored) {
            // Redis is a cache; the persistent route directory remains authoritative.
        }
    }

    private void rollbackBinding(RouteCoordinates coordinates, ShardRouteRecord pending,
            Optional<ShardRouteRecord> previous) {
        boolean removed = directory.removeIfOwned(coordinates.namespace(), coordinates.lookupHash(), pending.shardKey(),
                pending.version());
        if (removed && previous.isPresent()) {
            restore(coordinates, previous.get());
        } else if (removed) {
            evict(coordinates.cacheKey());
        }
    }

    private void restore(RouteCoordinates coordinates, ShardRouteRecord previous) {
        ShardRouteRecord restored = directory.bind(previous.namespace(), previous.lookupHash(), previous.shardKey(),
                previous.expiresAt(), false);
        cache(coordinates.cacheKey(), restored);
    }

    private void evict(String key) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (RuntimeException ignored) {
                // A stale cache entry expires naturally and cannot delete authoritative data.
            }
        }
    }

    private RouteCoordinates coordinates(String namespace, String lookupKey) {
        if (namespace == null || namespace.isBlank() || lookupKey == null || lookupKey.isBlank()) {
            throw new IllegalArgumentException("route namespace and lookup key must not be blank");
        }
        String lookupHash = sha256(lookupKey);
        return new RouteCoordinates(namespace, lookupHash, cacheKey(namespace, lookupHash));
    }

    private String cacheKey(String namespace, String lookupHash) {
        return KEY_PREFIX + namespace + ':' + lookupHash;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private record RouteCoordinates(String namespace, String lookupHash, String cacheKey) {
    }
}
