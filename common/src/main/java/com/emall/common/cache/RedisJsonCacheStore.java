package com.emall.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisJsonCacheStore<K, V> implements TwoLevelCache.CacheStore<K, V> {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final Class<V> valueType;
    private final Duration ttl;

    public RedisJsonCacheStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, String keyPrefix,
            Class<V> valueType, Duration ttl) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.keyPrefix = Objects.requireNonNull(keyPrefix, "keyPrefix must not be null");
        this.valueType = Objects.requireNonNull(valueType, "valueType must not be null");
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.ttl = ttl;
    }

    @Override
    public Optional<V> get(K key) {
        String payload = redisTemplate.opsForValue().get(redisKey(key));
        if (payload == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, valueType));
        } catch (JsonProcessingException ex) {
            evict(key);
            return Optional.empty();
        }
    }

    @Override
    public void put(K key, V value) {
        put(key, value, ttl);
    }

    @Override
    public void put(K key, V value, Duration entryTtl) {
        try {
            redisTemplate.opsForValue().set(redisKey(key), objectMapper.writeValueAsString(value),
                    entryTtl == null ? ttl : entryTtl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize cache value", ex);
        }
    }

    @Override
    public void evict(K key) {
        redisTemplate.delete(redisKey(key));
    }

    private String redisKey(K key) {
        return keyPrefix + ":" + key;
    }
}
