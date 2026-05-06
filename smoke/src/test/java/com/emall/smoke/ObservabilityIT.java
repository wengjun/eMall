package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ObservabilityIT {
    private static final Map<String, String> CORE_SERVICE_URLS = Map.of(
            "gateway", "EMALL_GATEWAY_URL|http://localhost:8080",
            "user", "EMALL_USER_URL|http://localhost:8081",
            "inventory", "EMALL_INVENTORY_URL|http://localhost:8083",
            "order", "EMALL_ORDER_URL|http://localhost:8084",
            "payment", "EMALL_PAYMENT_URL|http://localhost:8086",
            "fulfillment", "EMALL_FULFILLMENT_URL|http://localhost:8090"
    );

    @Test
    void shouldExposeHealthAndPrometheusMetricsForCoreServices() throws Exception {
        ProductionHttpGate.assumeEnabled("EMALL_RUN_OBSERVABILITY_IT");

        for (Map.Entry<String, String> entry : CORE_SERVICE_URLS.entrySet()) {
            String[] config = entry.getValue().split("\\|");
            String baseUrl = ProductionHttpGate.envOrDefault(config[0], config[1]);

            String health = ProductionHttpGate.getText(baseUrl, "/actuator/health");
            assertThat(health)
                    .as("%s health", entry.getKey())
                    .contains("\"status\"");

            String prometheus = ProductionHttpGate.getText(baseUrl, "/actuator/prometheus");
            assertThat(prometheus)
                    .as("%s prometheus", entry.getKey())
                    .contains("jvm")
                    .contains("http");
        }
    }
}
