package com.emall.common.security;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisTokenRevocationStore implements TokenRevocationStore {
    public static final String KEY_PREFIX = "emall:auth:revoked:";
    private final StringRedisTemplate redisTemplate;

    RedisTokenRevocationStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isRevoked(long sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + sessionId));
    }

    @Override
    public void revoke(long sessionId, Duration ttl) {
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, "1", ttl);
        }
    }
}
