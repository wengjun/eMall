package com.emall.traffic;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.controlplane.ControlPlaneClient;
import com.emall.common.controlplane.ControlPlaneCommands;
import com.emall.common.controlplane.ControlPlaneOperation;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"emall.storage=jdbc", "emall.control-plane.enabled=true",
        "emall.control-plane.nacos.enabled=false", "emall.control-plane.reconcile-delay=1h",
        "spring.flyway.enabled=true", "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false", "spring.kafka.listener.auto-startup=false"})
class TrafficControlPlaneIT {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4").withDatabaseName("emall_traffic")
            .withUsername("emall").withPassword("emall").withStartupTimeout(Duration.ofMinutes(2));

    @Autowired
    private ControlPlaneClient controlPlaneClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void persistsAndDeduplicatesControlPlaneOperation() {
        ControlPlaneOperation first = controlPlaneClient
                .submit(ControlPlaneCommands.nacosConfig("traffic-it-1", "traffic", "sync-routing", "routing-directory",
                        "multi-region", "routing.json", "CONTROL", "public", Map.of("activeRegion", "east")));
        ControlPlaneOperation replay = controlPlaneClient
                .submit(ControlPlaneCommands.nacosConfig("traffic-it-1", "traffic", "sync-routing", "routing-directory",
                        "multi-region", "routing.json", "CONTROL", "public", Map.of("activeRegion", "east")));

        Integer count =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM control_plane_operation WHERE idempotency_key = ?",
                        Integer.class, "traffic-it-1");
        assertThat(replay.operationId()).isEqualTo(first.operationId());
        assertThat(count).isOne();
    }
}
