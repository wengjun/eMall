package com.emall.common.sharding;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class ShardRouteIndex {
    private static final String KEY_PREFIX = "emall:shard-route:";
    private static final int MAXIMUM_LOCAL_ROUTES = 100_000;
    private static final DefaultRedisScript<Long> REMOVE_IF_OWNED_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> PERSIST_IF_OWNED_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                if redis.call('PTTL', KEYS[1]) == -1 then
                    return 1
                end
                return redis.call('PERSIST', KEYS[1])
            end
            return 0
            """, Long.class);
    private static final Duration PENDING_ROUTE_TTL = Duration.ofMinutes(10);
    private final StringRedisTemplate redisTemplate;
    private final boolean distributedRequired;
    private final ConcurrentMap<String, String> localRoutes = new ConcurrentHashMap<>();

    ShardRouteIndex(StringRedisTemplate redisTemplate, boolean distributedRequired) {
        this.redisTemplate = redisTemplate;
        this.distributedRequired = distributedRequired;
        if (distributedRequired && redisTemplate == null) {
            throw new IllegalStateException("Redis route index is required when sharding is enabled");
        }
    }

    public static ShardRouteIndex local() {
        return new ShardRouteIndex(null, false);
    }

    public void bind(String namespace, String lookupKey, long shardKey) {
        write(routeKey(namespace, lookupKey), Long.toString(shardKey), null, false);
    }

    public void bind(String namespace, String lookupKey, long shardKey, Duration ttl) {
        write(routeKey(namespace, lookupKey), Long.toString(shardKey), ttl, false);
    }

    public void bindUnique(String namespace, String lookupKey, long shardKey) {
        write(routeKey(namespace, lookupKey), Long.toString(shardKey), null, true);
    }

    public void bindUniqueTransactional(String namespace, String lookupKey, long shardKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            bindUnique(namespace, lookupKey, shardKey);
            return;
        }
        String key = routeKey(namespace, lookupKey);
        String value = Long.toString(shardKey);
        write(key, value, PENDING_ROUTE_TTL, true);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                persistIfOwned(key, value);
            }

            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    removeIfOwned(namespace, lookupKey, shardKey);
                }
            }
        });
    }

    public OptionalLong resolve(String namespace, String lookupKey) {
        String key = routeKey(namespace, lookupKey);
        String value;
        try {
            value = redisTemplate == null ? localRoutes.get(key) : redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ex) {
            if (distributedRequired) {
                throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "shard route index is unavailable");
            }
            value = localRoutes.get(key);
        }
        if (value == null) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Shard route index contains an invalid value", ex);
        }
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
        String key = routeKey(namespace, lookupKey);
        String expected = Long.toString(shardKey);
        if (redisTemplate != null) {
            try {
                redisTemplate.execute(REMOVE_IF_OWNED_SCRIPT, List.of(key), expected);
                return;
            } catch (RuntimeException ex) {
                if (distributedRequired) {
                    throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "shard route index is unavailable");
                }
            }
        }
        localRoutes.remove(key, expected);
    }

    public void removeIfOwnedTransactional(String namespace, String lookupKey, long shardKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            removeIfOwned(namespace, lookupKey, shardKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                removeIfOwned(namespace, lookupKey, shardKey);
            }

            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    bindUnique(namespace, lookupKey, shardKey);
                }
            }
        });
    }

    private void write(String key, String value, Duration ttl, boolean unique) {
        try {
            if (redisTemplate != null) {
                if (unique) {
                    Boolean created = ttl == null
                            ? redisTemplate.opsForValue().setIfAbsent(key, value)
                            : redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
                    if (!Boolean.TRUE.equals(created) && !value.equals(redisTemplate.opsForValue().get(key))) {
                        throw new BusinessException(ErrorCode.CONFLICT,
                                "global route key already belongs to another entity");
                    }
                } else if (ttl == null) {
                    redisTemplate.opsForValue().set(key, value);
                } else {
                    redisTemplate.opsForValue().set(key, value, ttl);
                }
                return;
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (distributedRequired) {
                throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "shard route index is unavailable");
            }
        }
        if (localRoutes.size() >= MAXIMUM_LOCAL_ROUTES && !localRoutes.containsKey(key)) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "local shard route index reached its limit");
        }
        if (unique) {
            String existing = localRoutes.putIfAbsent(key, value);
            if (existing != null && !existing.equals(value)) {
                throw new BusinessException(ErrorCode.CONFLICT, "global route key already belongs to another entity");
            }
        } else {
            localRoutes.put(key, value);
        }
    }

    private void persistIfOwned(String key, String value) {
        if (redisTemplate == null) {
            return;
        }
        try {
            Long persisted = redisTemplate.execute(PERSIST_IF_OWNED_SCRIPT, List.of(key), value);
            if (!Long.valueOf(1L).equals(persisted)) {
                throw new BusinessException(ErrorCode.CONFLICT, "global route ownership changed before commit");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (distributedRequired) {
                throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "shard route index is unavailable");
            }
        }
    }

    private String routeKey(String namespace, String lookupKey) {
        if (namespace == null || namespace.isBlank() || lookupKey == null || lookupKey.isBlank()) {
            throw new IllegalArgumentException("route namespace and lookup key must not be blank");
        }
        return KEY_PREFIX + namespace + ':' + sha256(lookupKey);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
