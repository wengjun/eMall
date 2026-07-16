package com.emall.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.sharding.ShardRouteRecord;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"spring.flyway.enabled=true", "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false", "emall.security.auth.enabled=false"})
class ShardRouteDirectoryIT {
    private static final String HASH = "a".repeat(64);
    private static final String CONFLICT_HASH = "b".repeat(64);

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4").withDatabaseName("emall_routing")
            .withUsername("emall").withPassword("emall").withStartupTimeout(Duration.ofMinutes(2));

    @Autowired
    private ShardRouteService service;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void persistsVersionedRoutesWithoutRedisAndFencesStaleDeletes() {
        Instant expiration = Instant.now().plus(Duration.ofDays(30));
        ShardRouteRecord created = service.bind("order-request", HASH, 1001L, expiration, true);
        ShardRouteRecord updated = service.bind("order-request", HASH, 1001L, expiration, true);

        assertThat(created.version()).isEqualTo(1L);
        assertThat(updated.version()).isEqualTo(2L);
        assertThat(service.resolve("order-request", HASH)).contains(updated);
        assertThat(service.removeIfOwned("order-request", HASH, 1001L, 1L)).isFalse();
        assertThat(service.removeIfOwned("order-request", HASH, 1001L, 2L)).isTrue();
        assertThat(service.resolve("order-request", HASH)).isEmpty();
    }

    @Test
    void rejectsUniqueRouteReassignmentAcrossEntities() {
        service.bind("order-id", CONFLICT_HASH, 1001L, null, true);

        assertThatThrownBy(() -> service.bind("order-id", CONFLICT_HASH, 2002L, null, true)).isInstanceOfSatisfying(
                BusinessException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT));
    }
}
