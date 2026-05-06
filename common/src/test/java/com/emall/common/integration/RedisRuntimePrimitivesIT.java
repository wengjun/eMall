package com.emall.common.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class RedisRuntimePrimitivesIT {
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @Test
    void shouldSupportCacheValuesAndRateLimitCounters() {
        LettuceConnectionFactory connectionFactory = connectionFactory();
        try {
            RedisTemplate<String, String> redisTemplate = redisTemplate(connectionFactory);

            redisTemplate.opsForValue().set("cache:product:30001", "flagship-phone", Duration.ofSeconds(30));
            Long firstHit = redisTemplate.opsForValue().increment("rate:gateway:client-001");
            Long secondHit = redisTemplate.opsForValue().increment("rate:gateway:client-001");
            Boolean ttlApplied = redisTemplate.expire("rate:gateway:client-001", Duration.ofSeconds(1));

            assertThat(redisTemplate.opsForValue().get("cache:product:30001")).isEqualTo("flagship-phone");
            assertThat(firstHit).isEqualTo(1L);
            assertThat(secondHit).isEqualTo(2L);
            assertThat(ttlApplied).isTrue();
            assertThat(redisTemplate.getExpire("rate:gateway:client-001")).isGreaterThan(0L);
        } finally {
            connectionFactory.destroy();
        }
    }

    private LettuceConnectionFactory connectionFactory() {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
                redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        return connectionFactory;
    }

    private RedisTemplate<String, String> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setValueSerializer(StringRedisSerializer.UTF_8);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
