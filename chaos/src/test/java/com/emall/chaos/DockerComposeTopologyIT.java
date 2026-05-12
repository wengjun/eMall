package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DockerComposeTopologyIT {
    private static final Path COMPOSE_FILE = Path.of("..", "docker-compose.yml").normalize();
    private static final Map<String, Integer> CORE_APPS =
            Map.ofEntries(Map.entry("user", 8081), Map.entry("product", 8082), Map.entry("inventory", 8083),
                    Map.entry("order", 8084), Map.entry("cart", 8085), Map.entry("payment", 8086),
                    Map.entry("pricing", 8087), Map.entry("marketing", 8088), Map.entry("search", 8089),
                    Map.entry("fulfillment", 8090), Map.entry("review", 8091), Map.entry("after-sales", 8092));

    @Test
    void shouldWireStableRuntimeServicesBehindGateway() throws IOException {
        String compose = Files.readString(COMPOSE_FILE);

        assertThat(compose).contains("  redis:").contains("  mysql:").contains("  kafka:").contains("  nacos:")
                .contains("  elasticsearch:").contains("  clickhouse:").contains("  logstash:").contains("  kibana:")
                .contains("  prometheus:").contains("  otel-collector:").contains("  grafana:")
                .contains("  gateway-app:").contains("EMALL_NACOS_DISCOVERY_ENABLED: \"true\"");

        for (Map.Entry<String, Integer> app : CORE_APPS.entrySet()) {
            String service = app.getKey();
            int port = app.getValue();
            assertThat(compose).as("compose app service for %s", service).contains("  " + service + "-app:")
                    .contains("MODULE: " + service).contains("\"" + port + ":" + port + "\"");
            assertThat(compose).as("gateway upstream for %s", service)
                    .contains(envName(service) + "_URL: lb://" + service)
                    .contains("      - " + service + "-app");
        }

        assertThat(compose).contains("EMALL_RPC_PROTOCOL: dubbo")
                .contains("EMALL_DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848");
    }

    private static String envName(String service) {
        return "EMALL_" + service.replace("-", "_").toUpperCase();
    }
}
