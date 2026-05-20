package com.emall.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisJsonCacheStoreTest {
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final RedisJsonCacheStore<String, CacheValue> store = new RedisJsonCacheStore<>(redisTemplate,
            new ObjectMapper(), "product", CacheValue.class, Duration.ofMinutes(5));

    @Test
    void shouldWriteSerializedValueWithEntryTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        store.put("10001", new CacheValue("phone", 2), Duration.ofSeconds(30));

        verify(valueOperations).set(eq("product:10001"), contains("\"name\":\"phone\""), eq(Duration.ofSeconds(30)));
    }

    @Test
    void shouldReadSerializedValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product:10001")).thenReturn("{\"name\":\"phone\",\"quantity\":2}");

        assertThat(store.get("10001")).contains(new CacheValue("phone", 2));
    }

    @Test
    void shouldEvictCorruptedJsonAndReturnMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product:10001")).thenReturn("{bad-json");

        assertThat(store.get("10001")).isEmpty();

        verify(redisTemplate).delete("product:10001");
    }

    @Test
    void shouldRejectNonPositiveTtl() {
        assertThatThrownBy(() -> new RedisJsonCacheStore<>(redisTemplate, new ObjectMapper(), "product",
                CacheValue.class, Duration.ZERO)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttl");
    }

    private record CacheValue(String name, int quantity) {
    }
}
