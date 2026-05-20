package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;

class CostGovernanceContractIT {
    @Test
    void shouldExposeCapacityGovernanceContractAgainstRunningService() throws Exception {
        ProductionHttpGate.assumeEnabled("EMALL_RUN_COST_GOVERNANCE_IT");
        String baseUrl = ProductionHttpGate.envOrDefault("EMALL_COST_URL", "http://localhost:8096");
        String serviceName = "payment-contract-" + System.currentTimeMillis();

        JsonNode baseline = ProductionHttpGate.postJson(baseUrl, "/api/cost/capacity-baselines",
                Map.ofEntries(entry("serviceName", serviceName), entry("safeQps", 100000), entry("peakQps", 150000),
                        entry("currentQps", 95000), entry("currentReplicas", 8), entry("maxReplicas", 20),
                        entry("cpuUtilization", new BigDecimal("0.65")),
                        entry("memoryUtilization", new BigDecimal("0.62")),
                        entry("monthlyCost", new BigDecimal("18000")), entry("sloProtected", true),
                        entry("observedAt", Instant.now())),
                null);
        JsonNode capacity = ProductionHttpGate.getJson(baseUrl, "/api/cost/services/" + serviceName + "/capacity");

        assertThat(baseline.path("data").path("riskLevel").asText()).isEqualTo("SCALE_OUT_REQUIRED");
        assertThat(capacity.path("data").path("riskLevel").asText()).isEqualTo("SCALE_OUT_REQUIRED");
        assertThat(capacity.path("data").path("activeActions").asInt()).isGreaterThanOrEqualTo(1);
    }

    private static Entry<String, Object> entry(String key, Object value) {
        return Map.entry(key, value);
    }
}
