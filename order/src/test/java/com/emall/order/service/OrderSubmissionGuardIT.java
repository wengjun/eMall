package com.emall.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class OrderSubmissionGuardIT {
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
    void enforcesOneExpiringLimitAcrossIndependentInstances() throws Exception {
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        try {
            StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
            redis.afterPropertiesSet();
            Duration window = Duration.ofMillis(500);
            OrderSubmissionGuard first = new OrderSubmissionGuard(2, window, redis, true);
            OrderSubmissionGuard second = new OrderSubmissionGuard(2, window, redis, true);

            first.check(70001L);
            second.check(70001L);
            assertThatThrownBy(() -> first.check(70001L)).isInstanceOf(BusinessException.class)
                    .hasMessageContaining("too many order submissions");
            assertThat(redis.getExpire("emall:order:submission:{70001}")).isPositive();

            Thread.sleep(window.multipliedBy(2).plusMillis(200).toMillis());
            assertThatCode(() -> second.check(70001L)).doesNotThrowAnyException();
        } finally {
            connectionFactory.destroy();
        }
    }

    private static void requireDocker() {
        boolean available = DockerClientFactory.instance().isDockerAvailable();
        if (!available && Boolean.getBoolean("emall.integration.require-docker")) {
            throw new IllegalStateException("Docker is required for production integration tests");
        }
        Assumptions.assumeTrue(available, "Docker is unavailable");
    }
}
