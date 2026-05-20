package com.emall.flashsale.runtime;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(StringRedisTemplate.class)
@ConditionalOnProperty(name = "emall.flash-sale.runtime", havingValue = "redis")
public class RedisFlashSaleRuntimeStore implements FlashSaleRuntimeStore {
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
            local reserved = tonumber(redis.call('GET', KEYS[2]) or '0')
            local quantity = tonumber(ARGV[1])
            local userLimit = tonumber(ARGV[2])
            local ttl = tonumber(ARGV[3])
            if stock < quantity or reserved + quantity > userLimit then
                return 0
            end
            redis.call('DECRBY', KEYS[1], quantity)
            redis.call('INCRBY', KEYS[2], quantity)
            redis.call('EXPIRE', KEYS[2], ttl)
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> ENQUEUE_SCRIPT = new DefaultRedisScript<>("""
            local queued = tonumber(redis.call('GET', KEYS[1]) or '0')
            local quantity = tonumber(ARGV[1])
            local capacity = tonumber(ARGV[2])
            local ttl = tonumber(ARGV[3])
            if queued + quantity > capacity then
                return 0
            end
            if redis.call('SETNX', KEYS[2], '1') == 0 then
                return 0
            end
            redis.call('EXPIRE', KEYS[2], ttl)
            redis.call('INCRBY', KEYS[1], quantity)
            redis.call('RPUSH', KEYS[3], ARGV[4])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            local reserved = tonumber(redis.call('GET', KEYS[2]) or '0')
            local quantity = tonumber(ARGV[1])
            redis.call('INCRBY', KEYS[1], quantity)
            local nextReserved = reserved - quantity
            if nextReserved <= 0 then
                redis.call('DEL', KEYS[2])
            else
                redis.call('SET', KEYS[2], tostring(nextReserved))
            end
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE_QUEUE_SCRIPT = new DefaultRedisScript<>("""
            local quantity = tonumber(ARGV[1])
            local queued = tonumber(redis.call('GET', KEYS[1]) or '0')
            local nextQueued = queued - quantity
            if nextQueued < 0 then
                nextQueued = 0
            end
            redis.call('SET', KEYS[1], tostring(nextQueued))
            redis.call('DEL', KEYS[2])
            redis.call('LREM', KEYS[3], 0, ARGV[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisFlashSaleRuntimeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void preloadStock(long campaignId, int totalStock) {
        redisTemplate.opsForValue().set(stockKey(campaignId), Integer.toString(totalStock), Duration.ofDays(2));
        redisTemplate.opsForValue().set(queuedKey(campaignId), "0", Duration.ofDays(2));
    }

    @Override
    public boolean reserveTokenStock(long campaignId, long userId, int quantity, int perUserLimit, long ttlSeconds) {
        Long result = redisTemplate.execute(RESERVE_SCRIPT, List.of(stockKey(campaignId), userKey(campaignId, userId)),
                Integer.toString(quantity), Integer.toString(perUserLimit), Long.toString(ttlSeconds));
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public void releaseTokenStock(long campaignId, long userId, int quantity) {
        redisTemplate.execute(RELEASE_TOKEN_SCRIPT, List.of(stockKey(campaignId), userKey(campaignId, userId)),
                Integer.toString(quantity));
    }

    @Override
    public boolean enqueueToken(long campaignId, String token, int quantity, int queueCapacity, long ttlSeconds) {
        Long result = redisTemplate.execute(ENQUEUE_SCRIPT,
                List.of(queuedKey(campaignId), usedTokenKey(token), queueKey(campaignId)), Integer.toString(quantity),
                Integer.toString(queueCapacity), Long.toString(ttlSeconds), token);
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public void releaseQueuedToken(long campaignId, String token, int quantity) {
        redisTemplate.execute(RELEASE_QUEUE_SCRIPT,
                List.of(queuedKey(campaignId), usedTokenKey(token), queueKey(campaignId)), Integer.toString(quantity),
                token);
    }

    private String stockKey(long campaignId) {
        return "flash-sale:stock:" + campaignId;
    }

    private String queuedKey(long campaignId) {
        return "flash-sale:queued:" + campaignId;
    }

    private String queueKey(long campaignId) {
        return "flash-sale:queue:" + campaignId;
    }

    private String usedTokenKey(String token) {
        return "flash-sale:used-token:" + token;
    }

    private String userKey(long campaignId, long userId) {
        return "flash-sale:user:" + campaignId + ":" + userId;
    }
}
