package com.emall.routing;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.sharding.ShardRoutePage;
import com.emall.common.sharding.ShardRouteDirectory;
import com.emall.common.sharding.ShardRouteRecord;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShardRouteService implements ShardRouteDirectory {
    private static final int MAXIMUM_CAS_ATTEMPTS = 8;
    private final ShardRouteMapper mapper;
    private final Clock clock;

    @Autowired
    public ShardRouteService(ShardRouteMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    ShardRouteService(ShardRouteMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ShardRouteRecord> resolve(String namespace, String lookupHash) {
        validate(namespace, lookupHash);
        ShardRouteEntity entity = mapper.selectById(routeId(namespace, lookupHash));
        if (entity == null || expired(entity)) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Transactional
    @Override
    public ShardRouteRecord bind(String namespace, String lookupHash, long shardKey, Instant expiresAt,
            boolean unique) {
        validate(namespace, lookupHash);
        Instant normalizedExpiration = databasePrecision(expiresAt);
        String routeId = routeId(namespace, lookupHash);
        for (int attempt = 0; attempt < MAXIMUM_CAS_ATTEMPTS; attempt++) {
            ShardRouteEntity current = mapper.selectById(routeId);
            if (current == null) {
                Instant now = databasePrecision(clock.instant());
                ShardRouteEntity created =
                        entity(routeId, namespace, lookupHash, shardKey, 1L, normalizedExpiration, now, now);
                try {
                    mapper.insert(created);
                    return toDomain(created);
                } catch (DuplicateKeyException ignored) {
                    continue;
                }
            }
            if (!expired(current) && unique && current.getShardKey() != shardKey) {
                throw new BusinessException(ErrorCode.CONFLICT, "global route key already belongs to another entity");
            }
            Instant now = databasePrecision(clock.instant());
            long nextVersion = current.getRouteVersion() + 1;
            int updated = mapper.update(null,
                    new UpdateWrapper<ShardRouteEntity>().set("shard_key", shardKey).set("route_version", nextVersion)
                            .set("expires_at", databaseTime(normalizedExpiration)).set("updated_at", databaseTime(now))
                            .eq("route_id", routeId).eq("route_version", current.getRouteVersion()));
            if (updated == 1) {
                return new ShardRouteRecord(namespace, lookupHash, shardKey, nextVersion, normalizedExpiration,
                        domainTime(current.getCreatedAt()), now);
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT, "global route changed too frequently; retry the operation");
    }

    @Transactional
    @Override
    public boolean removeIfOwned(String namespace, String lookupHash, long shardKey, Long expectedVersion) {
        validate(namespace, lookupHash);
        QueryWrapper<ShardRouteEntity> query = new QueryWrapper<ShardRouteEntity>()
                .eq("route_id", routeId(namespace, lookupHash)).eq("shard_key", shardKey);
        if (expectedVersion != null) {
            query.eq("route_version", expectedVersion);
        }
        return mapper.delete(query) == 1;
    }

    @Transactional(readOnly = true)
    @Override
    public ShardRoutePage scan(String cursor, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        LocalDateTime now = databaseTime(clock.instant());
        QueryWrapper<ShardRouteEntity> query = new QueryWrapper<ShardRouteEntity>()
                .and(active -> active.isNull("expires_at").or().gt("expires_at", now)).orderByAsc("route_id")
                .last("LIMIT " + (boundedLimit + 1));
        if (cursor != null && !cursor.isBlank()) {
            query.gt("route_id", cursor);
        }
        List<ShardRouteEntity> entities = mapper.selectList(query);
        boolean hasMore = entities.size() > boundedLimit;
        List<ShardRouteEntity> page = hasMore ? entities.subList(0, boundedLimit) : entities;
        String nextCursor = hasMore ? page.get(page.size() - 1).getRouteId() : null;
        return new ShardRoutePage(page.stream().map(this::toDomain).toList(), nextCursor);
    }

    private ShardRouteEntity entity(String routeId, String namespace, String lookupHash, long shardKey, long version,
            Instant expiresAt, Instant createdAt, Instant updatedAt) {
        ShardRouteEntity entity = new ShardRouteEntity();
        entity.setRouteId(routeId);
        entity.setNamespace(namespace);
        entity.setLookupHash(lookupHash);
        entity.setShardKey(shardKey);
        entity.setRouteVersion(version);
        entity.setExpiresAt(databaseTime(expiresAt));
        entity.setCreatedAt(databaseTime(createdAt));
        entity.setUpdatedAt(databaseTime(updatedAt));
        return entity;
    }

    private ShardRouteRecord toDomain(ShardRouteEntity entity) {
        return new ShardRouteRecord(entity.getNamespace(), entity.getLookupHash(), entity.getShardKey(),
                entity.getRouteVersion(), domainTime(entity.getExpiresAt()), domainTime(entity.getCreatedAt()),
                domainTime(entity.getUpdatedAt()));
    }

    private boolean expired(ShardRouteEntity entity) {
        return entity.getExpiresAt() != null && !domainTime(entity.getExpiresAt()).isAfter(clock.instant());
    }

    private void validate(String namespace, String lookupHash) {
        if (namespace == null || !namespace.matches("[a-z0-9-]{1,64}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "route namespace is invalid");
        }
        if (lookupHash == null || !lookupHash.matches("[a-f0-9]{64}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "route lookup hash is invalid");
        }
    }

    private String routeId(String namespace, String lookupHash) {
        return namespace + ':' + lookupHash;
    }

    private LocalDateTime databaseTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant domainTime(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private Instant databasePrecision(Instant value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.MICROS);
    }
}
