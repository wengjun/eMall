package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ObservabilityConfigIT {
    @Test
    void shouldCoverPrometheusAlertsGrafanaAndOtelCollector() throws IOException {
        String prometheus = Files.readString(Path.of("..", "ops", "prometheus.yml").normalize());
        String alerts = Files.readString(Path.of("..", "ops", "prometheus", "rules", "emall-alerts.yml").normalize());
        String otel = Files.readString(Path.of("..", "ops", "otel-collector.yml").normalize());
        String dashboard =
                Files.readString(Path.of("..", "ops", "grafana", "dashboards", "emall-overview.json").normalize());

        assertThat(prometheus).contains("scrape_interval: 15s").contains("metrics_path: /actuator/prometheus")
                .contains("/etc/prometheus/rules/*.yml");
        for (int port = 8080; port <= 8116; port++) {
            assertThat(prometheus).contains("host.docker.internal:" + port);
        }

        assertThat(alerts).contains("EmallServiceDown").contains("EmallHighHttp5xxRate")
                .contains("EmallHighHttpP95Latency").contains("EmallKafkaConsumerLagHigh")
                .contains("EmallHpaNearMaxReplicas").contains("EmallPlatformOpsCriticalSecuritySignalsHigh");
        assertThat(otel).contains("endpoint: 0.0.0.0:4317").contains("endpoint: 0.0.0.0:4318").contains("processors:")
                .contains("exporters:");
        assertThat(dashboard).contains("\"uid\": \"emall-overview\"").contains("\"title\": \"HTTP QPS\"")
                .contains("\"title\": \"HTTP 5xx Ratio\"").contains("\"title\": \"HTTP P95 Latency\"")
                .contains("\"title\": \"JVM Heap Usage\"");
    }
}
