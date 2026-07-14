package com.emall.common.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisWorkerIdLeaseIT {
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine")).withExposedPorts(6379);

    @BeforeAll
    static void startRedis() {
        requireDocker();
        REDIS.start();
    }

    @AfterAll
    static void stopRedis() {
        if (REDIS.isRunning()) {
            REDIS.stop();
        }
    }

    @Test
    void preventsDuplicateWorkersAndReusesIdOnlyAfterLeaseCooldown() throws Exception {
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        try {
            StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
            redis.afterPropertiesSet();
            SnowflakeIdProperties properties = properties(77L);
            RedisWorkerIdLease first = new RedisWorkerIdLease(redis, "pod-a", properties);

            assertThatThrownBy(() -> new RedisWorkerIdLease(redis, "pod-b", properties))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("already leased");
            first.close();
            assertThatThrownBy(() -> new RedisWorkerIdLease(redis, "pod-c", properties))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("already leased");

            Thread.sleep(properties.getLeaseTtl().plusMillis(300).toMillis());
            try (RedisWorkerIdLease replacement = new RedisWorkerIdLease(redis, "pod-d", properties)) {
                assertThat(replacement.workerId()).isEqualTo(77L);
                new SnowflakeIdGenerator(replacement, 5L).nextId();
            }
        } finally {
            connectionFactory.destroy();
        }
    }

    private SnowflakeIdProperties properties(long workerId) {
        SnowflakeIdProperties properties = new SnowflakeIdProperties();
        properties.setWorkerId(workerId);
        properties.setLeaseTtl(Duration.ofSeconds(2));
        properties.setRenewInterval(Duration.ofMillis(500));
        return properties;
    }

    private static void requireDocker() {
        boolean available = DockerClientFactory.instance().isDockerAvailable();
        if (!available && Boolean.getBoolean("emall.integration.require-docker")) {
            throw new IllegalStateException("Docker is required for production integration tests");
        }
        Assumptions.assumeTrue(available, "Docker is unavailable");
    }
}
