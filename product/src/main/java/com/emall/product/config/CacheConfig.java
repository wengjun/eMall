package com.emall.product.config;

import com.emall.common.cache.CacheTtlPolicy;
import com.emall.common.cache.ExpiringMapCacheStore;
import com.emall.common.cache.RedisJsonCacheStore;
import com.emall.common.cache.TwoLevelCache;
import com.emall.product.service.ProductCacheEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class CacheConfig {
    @Bean
    RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
            @Value("${emall.cache.ttl}") Duration ttl,
            @Value("${emall.cache.ttl-jitter-ratio:0.1}") double jitterRatio) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(new CacheTtlPolicy(ttl, jitterRatio).ttlForKey("product-cache")).disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(configuration).build();
    }

    @Bean
    TwoLevelCache<Long, ProductCacheEntry> productDetailCache(StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper, @Value("${emall.cache.ttl}") Duration ttl) {
        return new TwoLevelCache<>(new ExpiringMapCacheStore<>(Duration.ofSeconds(30)), new RedisJsonCacheStore<>(
                redisTemplate, objectMapper, "emall:product:detail", ProductCacheEntry.class, ttl));
    }
}
