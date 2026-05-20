package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReleaseTrafficContractIT {
    @Test
    void shouldExposeReleaseAndTrafficGovernanceContractsAgainstRunningServices() throws Exception {
        ProductionHttpGate.assumeEnabled("EMALL_RUN_RELEASE_TRAFFIC_IT");
        String releaseBaseUrl = ProductionHttpGate.envOrDefault("EMALL_RELEASE_URL", "http://localhost:8115");
        String trafficBaseUrl = ProductionHttpGate.envOrDefault("EMALL_TRAFFIC_URL", "http://localhost:8113");
        String suffix = String.valueOf(System.currentTimeMillis());

        JsonNode toggle = ProductionHttpGate.postJson(releaseBaseUrl, "/api/release/feature-toggles", Map.of("flagKey",
                "checkout-new-flow-" + suffix, "serviceName", "order", "status", "ON", "rolloutPercent", 25), null);
        JsonNode rollout = ProductionHttpGate.postJson(releaseBaseUrl, "/api/release/rollouts",
                Map.of("serviceName", "payment", "version", "v" + suffix, "strategy", "canary", "currentPercent", 0),
                null);
        long rolloutId = rollout.path("data").path("rolloutId").asLong();
        JsonNode preTrafficGuard = ProductionHttpGate.postJson(releaseBaseUrl,
                "/api/release/rollouts/" + rolloutId + "/guards/pre-traffic",
                Map.of("sloPassed", true, "alertsClear", true, "capacityReady", true, "dependenciesHealthy", true),
                null);
        JsonNode canaryGuard = ProductionHttpGate.postJson(releaseBaseUrl,
                "/api/release/rollouts/" + rolloutId + "/guards/canary", Map.of("observedPercent", 20, "errorRate",
                        new BigDecimal("0.005"), "latencyP95Ms", 200, "businessSuccessRate", new BigDecimal("0.999")),
                null);
        JsonNode topic = ProductionHttpGate.postJson(releaseBaseUrl, "/api/release/topics", Map.of("topicName",
                "order-paid-" + suffix, "owner", "platform", "schemaVersion", "v1", "lagBudget", 1000L), null);
        ProductionHttpGate.postJson(releaseBaseUrl, "/api/release/replay-plans",
                Map.of("topicName", topic.path("data").path("topicName").asText(), "consumerGroup", "search-indexer",
                        "fromOffset", 0L, "toOffset", 100L),
                null);

        assertThat(toggle.path("data").path("status").asText()).isEqualTo("ON");
        assertThat(preTrafficGuard.path("data").path("decision").asText()).isEqualTo("PASS");
        assertThat(canaryGuard.path("data").path("decision").asText()).isEqualTo("PASS");
        JsonNode releaseSummary = ProductionHttpGate.getJson(releaseBaseUrl, "/api/release/summary");
        assertThat(releaseSummary.path("data").path("enabledFlags").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(releaseSummary.path("data").path("runningRollouts").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(releaseSummary.path("data").path("activeTopics").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(releaseSummary.path("data").path("openReplays").asInt()).isGreaterThanOrEqualTo(1);

        ProductionHttpGate.postJson(trafficBaseUrl, "/api/traffic/units",
                Map.of("unitCode", "cell-a-" + suffix, "regionCode", "cn-east-1", "capacityWeight", 100), null);
        ProductionHttpGate.postJson(trafficBaseUrl, "/api/traffic/units",
                Map.of("unitCode", "cell-b-" + suffix, "regionCode", "cn-south-1", "capacityWeight", 80), null);
        ProductionHttpGate.postJson(trafficBaseUrl, "/api/traffic/shard-routes",
                Map.of("domainName", "order", "shardNo", 3, "unitCode", "cell-a-" + suffix, "databaseKey", "order_03"),
                null);
        JsonNode shift = ProductionHttpGate.postJson(trafficBaseUrl, "/api/traffic/shifts", Map.of("sourceUnit",
                "cell-a-" + suffix, "targetUnit", "cell-b-" + suffix, "percent", 20, "reason", "canary relocation"),
                null);
        ProductionHttpGate.patchJson(trafficBaseUrl,
                "/api/traffic/shifts/" + shift.path("data").path("shiftId").asLong() + "/status",
                Map.of("status", "RUNNING"), null);
        JsonNode rule = ProductionHttpGate.postJson(trafficBaseUrl, "/api/traffic/control-rules",
                Map.of("resource", "checkout", "type", "RATE_LIMIT", "dimension", "userTier", "matchValue", "new",
                        "threshold", 1000, "unitCode", "cell-a-" + suffix, "enabled", true),
                null);
        ProductionHttpGate.patchJson(trafficBaseUrl,
                "/api/traffic/control-rules/" + rule.path("data").path("ruleId").asLong() + "/enabled",
                Map.of("enabled", false), null);
        ProductionHttpGate.patchJson(trafficBaseUrl, "/api/traffic/units/cell-b-" + suffix + "/isolate", Map.of(),
                null);

        JsonNode trafficSummary = ProductionHttpGate.getJson(trafficBaseUrl, "/api/traffic/summary");
        assertThat(trafficSummary.path("data").path("activeUnits").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(trafficSummary.path("data").path("shardRoutes").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(trafficSummary.path("data").path("runningShifts").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(trafficSummary.path("data").path("isolatedUnits").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(trafficSummary.path("data").path("controlRules").asInt()).isGreaterThanOrEqualTo(1);
    }
}
