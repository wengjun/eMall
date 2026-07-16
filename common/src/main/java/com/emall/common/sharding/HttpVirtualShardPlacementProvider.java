package com.emall.common.sharding;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

public final class HttpVirtualShardPlacementProvider implements VirtualShardPlacementProvider {
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private final RestClient client;
    private final VirtualShardPlacementProvider fallback;
    private final Duration cacheTtl;
    private final Duration staleReadTtl;
    private final int virtualShardCount;
    private final Clock clock;
    private final ConcurrentMap<PlacementKey, CachedPlacement> cache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CachedSnapshot> snapshots = new ConcurrentHashMap<>();

    public HttpVirtualShardPlacementProvider(RestClient.Builder builder, String endpoint, String operationsToken,
            VirtualShardPlacementProvider fallback, Duration cacheTtl, Duration staleReadTtl) {
        this(builder, endpoint, operationsToken, fallback, cacheTtl, staleReadTtl, 4096, Clock.systemUTC());
    }

    public HttpVirtualShardPlacementProvider(RestClient.Builder builder, String endpoint, String operationsToken,
            VirtualShardPlacementProvider fallback, Duration cacheTtl, Duration staleReadTtl, int virtualShardCount) {
        this(builder, endpoint, operationsToken, fallback, cacheTtl, staleReadTtl, virtualShardCount,
                Clock.systemUTC());
    }

    HttpVirtualShardPlacementProvider(RestClient.Builder builder, String endpoint, String operationsToken,
            VirtualShardPlacementProvider fallback, Duration cacheTtl, Duration staleReadTtl, Clock clock) {
        this(builder, endpoint, operationsToken, fallback, cacheTtl, staleReadTtl, 4096, clock);
    }

    HttpVirtualShardPlacementProvider(RestClient.Builder builder, String endpoint, String operationsToken,
            VirtualShardPlacementProvider fallback, Duration cacheTtl, Duration staleReadTtl, int virtualShardCount,
            Clock clock) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("virtual shard control endpoint must not be blank");
        }
        if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero() || staleReadTtl == null
                || staleReadTtl.compareTo(cacheTtl) < 0) {
            throw new IllegalArgumentException("mapping cache and stale-read TTL values are invalid");
        }
        if (virtualShardCount <= 0 || Integer.bitCount(virtualShardCount) != 1) {
            throw new IllegalArgumentException("virtual shard count must be a positive power of two");
        }
        this.client = builder.clone().baseUrl(endpoint).defaultHeader(INTERNAL_TOKEN_HEADER, operationsToken).build();
        this.fallback = fallback;
        this.cacheTtl = cacheTtl;
        this.staleReadTtl = staleReadTtl;
        this.virtualShardCount = virtualShardCount;
        this.clock = clock;
    }

    @Override
    public VirtualShardPlacement resolve(String namespace, int virtualShard, ShardAccessMode accessMode) {
        PlacementKey key = new PlacementKey(namespace, virtualShard);
        Instant now = clock.instant();
        CachedPlacement cached = cache.get(key);
        if (cached != null && cached.freshAt(now)) {
            return authorize(cached.placement(), accessMode);
        }
        try {
            PlacementResponse response =
                    client.get().uri("/internal/virtual-shards/{namespace}/{virtualShard}", namespace, virtualShard)
                            .retrieve().body(PlacementResponse.class);
            VirtualShardPlacement placement = response == null || response.data() == null
                    ? fallback.resolve(namespace, virtualShard, accessMode)
                    : response.data();
            validate(key, placement);
            cache.put(key, new CachedPlacement(placement, now, now.plus(cacheTtl)));
            return authorize(placement, accessMode);
        } catch (HttpClientErrorException.NotFound ex) {
            VirtualShardPlacement placement = fallback.resolve(namespace, virtualShard, accessMode);
            cache.put(key, new CachedPlacement(placement, now, now.plus(cacheTtl)));
            return authorize(placement, accessMode);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (accessMode == ShardAccessMode.READ && cached != null && cached.readableAt(now, staleReadTtl)) {
                return cached.placement();
            }
            throw unavailable(ex);
        }
    }

    @Override
    public List<PhysicalShardPlacement> activePhysicalPlacements(String namespace, String logicalTable,
            ShardAccessMode accessMode) {
        Instant now = clock.instant();
        CachedSnapshot cached = snapshots.get(namespace);
        if (cached != null && cached.freshAt(now)) {
            return merge(namespace, logicalTable, accessMode, cached.placements());
        }
        try {
            PlacementListResponse response = client.get()
                    .uri(builder -> builder.path("/internal/virtual-shards").queryParam("namespace", namespace).build())
                    .retrieve().body(PlacementListResponse.class);
            List<VirtualShardPlacement> placements =
                    response == null || response.data() == null ? List.of() : List.copyOf(response.data());
            CachedSnapshot refreshed = new CachedSnapshot(placements, now, now.plus(cacheTtl));
            snapshots.put(namespace, refreshed);
            placements.forEach(placement -> cache.put(new PlacementKey(namespace, placement.virtualShard()),
                    new CachedPlacement(placement, now, now.plus(cacheTtl))));
            return merge(namespace, logicalTable, accessMode, placements);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (accessMode == ShardAccessMode.READ && cached != null && cached.readableAt(now, staleReadTtl)) {
                return merge(namespace, logicalTable, accessMode, cached.placements());
            }
            throw unavailable(ex);
        }
    }

    @Override
    public void invalidate(String namespace, int virtualShard) {
        cache.remove(new PlacementKey(namespace, virtualShard));
        snapshots.remove(namespace);
    }

    private List<PhysicalShardPlacement> merge(String namespace, String logicalTable, ShardAccessMode accessMode,
            List<VirtualShardPlacement> overrides) {
        Map<Integer, VirtualShardPlacement> overridesByShard = new LinkedHashMap<>();
        for (VirtualShardPlacement override : overrides) {
            validate(new PlacementKey(namespace, override.virtualShard()), override);
            overridesByShard.put(override.virtualShard(), override);
        }
        Map<String, PhysicalShardPlacement> active = new LinkedHashMap<>();
        for (int virtualShard = 0; virtualShard < virtualShardCount; virtualShard++) {
            VirtualShardPlacement override = overridesByShard.get(virtualShard);
            VirtualShardPlacement placement =
                    override == null ? fallback.resolve(namespace, virtualShard, accessMode) : override;
            if (accessMode == ShardAccessMode.WRITE) {
                placement.requireWriteAllowed();
            }
            active.put(placement.primary().identity(logicalTable), placement.primary());
        }
        return List.copyOf(active.values());
    }

    private VirtualShardPlacement authorize(VirtualShardPlacement placement, ShardAccessMode accessMode) {
        if (accessMode == ShardAccessMode.WRITE) {
            placement.requireWriteAllowed();
        }
        return placement;
    }

    private void validate(PlacementKey key, VirtualShardPlacement placement) {
        if (!key.namespace().equals(placement.namespace()) || key.virtualShard() != placement.virtualShard()) {
            throw new IllegalStateException("virtual shard control plane returned mismatched placement coordinates");
        }
    }

    private BusinessException unavailable(RuntimeException cause) {
        BusinessException exception = new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE,
                "virtual shard mapping control plane is unavailable");
        exception.initCause(cause);
        return exception;
    }

    private record PlacementKey(String namespace, int virtualShard) {
    }

    private record CachedPlacement(VirtualShardPlacement placement, Instant refreshedAt, Instant expiresAt) {
        private boolean freshAt(Instant now) {
            return now.isBefore(expiresAt);
        }

        private boolean readableAt(Instant now, Duration staleReadTtl) {
            return now.isBefore(refreshedAt.plus(staleReadTtl));
        }
    }

    private record CachedSnapshot(List<VirtualShardPlacement> placements, Instant refreshedAt, Instant expiresAt) {
        private boolean freshAt(Instant now) {
            return now.isBefore(expiresAt);
        }

        private boolean readableAt(Instant now, Duration staleReadTtl) {
            return now.isBefore(refreshedAt.plus(staleReadTtl));
        }
    }

    private record PlacementResponse(boolean success, String code, String message, VirtualShardPlacement data,
            Instant timestamp) {
    }

    private record PlacementListResponse(boolean success, String code, String message, List<VirtualShardPlacement> data,
            Instant timestamp) {
    }
}
