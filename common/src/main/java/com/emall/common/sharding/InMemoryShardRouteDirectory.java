package com.emall.common.sharding;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryShardRouteDirectory implements ShardRouteDirectory {
    private final ConcurrentMap<RouteKey, ShardRouteRecord> routes = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryShardRouteDirectory() {
        this(Clock.systemUTC());
    }

    InMemoryShardRouteDirectory(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<ShardRouteRecord> resolve(String namespace, String lookupHash) {
        RouteKey key = new RouteKey(namespace, lookupHash);
        ShardRouteRecord route = routes.get(key);
        if (route == null) {
            return Optional.empty();
        }
        if (route.expired(clock.instant())) {
            routes.remove(key, route);
            return Optional.empty();
        }
        return Optional.of(route);
    }

    @Override
    public ShardRouteRecord bind(String namespace, String lookupHash, long shardKey, Instant expiresAt,
            boolean unique) {
        RouteKey key = new RouteKey(namespace, lookupHash);
        return routes.compute(key, (ignored, existing) -> {
            Instant now = clock.instant();
            if (existing != null && !existing.expired(now) && unique && existing.shardKey() != shardKey) {
                throw new BusinessException(ErrorCode.CONFLICT, "global route key already belongs to another entity");
            }
            long nextVersion = existing == null ? 1L : existing.version() + 1;
            Instant createdAt = existing == null ? now : existing.createdAt();
            return new ShardRouteRecord(namespace, lookupHash, shardKey, nextVersion, expiresAt, createdAt, now);
        });
    }

    @Override
    public boolean removeIfOwned(String namespace, String lookupHash, long shardKey, Long expectedVersion) {
        RouteKey key = new RouteKey(namespace, lookupHash);
        ShardRouteRecord current = routes.get(key);
        boolean versionMatches = expectedVersion == null || current != null && current.version() == expectedVersion;
        return current != null && versionMatches && current.shardKey() == shardKey && routes.remove(key, current);
    }

    @Override
    public ShardRoutePage scan(String cursor, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        List<ShardRouteRecord> sorted = routes.values().stream().filter(route -> !route.expired(clock.instant()))
                .sorted(Comparator.comparing(this::cursor))
                .filter(route -> cursor == null || cursor(route).compareTo(cursor) > 0).limit(boundedLimit + 1L)
                .toList();
        boolean hasMore = sorted.size() > boundedLimit;
        List<ShardRouteRecord> page = hasMore ? sorted.subList(0, boundedLimit) : sorted;
        String nextCursor = hasMore ? cursor(page.get(page.size() - 1)) : null;
        return new ShardRoutePage(page, nextCursor);
    }

    private String cursor(ShardRouteRecord route) {
        return route.namespace() + ':' + route.lookupHash();
    }

    private record RouteKey(String namespace, String lookupHash) {
    }
}
