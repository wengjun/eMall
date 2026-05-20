package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FlashSaleContractIT {
    @Test
    void shouldIssueIdempotentTokenAndQueueOrderAgainstRunningService() throws Exception {
        ProductionHttpGate.assumeEnabled("EMALL_RUN_FLASH_SALE_IT");
        String baseUrl = ProductionHttpGate.envOrDefault("EMALL_FLASH_SALE_URL", "http://localhost:8094");
        String suffix = String.valueOf(System.currentTimeMillis());
        Instant now = Instant.now();

        JsonNode campaign = ProductionHttpGate.postJson(baseUrl, "/api/flash-sales/campaigns",
                Map.of("skuId", Long.parseLong("9" + suffix.substring(suffix.length() - 8)), "name",
                        "flash sale contract " + suffix, "startsAt", now.minus(1, ChronoUnit.MINUTES), "endsAt",
                        now.plus(30, ChronoUnit.MINUTES), "perUserLimit", 2, "tokenTtlSeconds", 120, "queueCapacity",
                        10),
                null);
        long campaignId = campaign.path("data").path("campaignId").asLong();
        ProductionHttpGate.patchJson(baseUrl, "/api/flash-sales/campaigns/" + campaignId + "/status",
                Map.of("status", "ACTIVE"), null);
        ProductionHttpGate.postJson(baseUrl, "/api/flash-sales/campaigns/" + campaignId + "/stock",
                Map.of("totalStock", 5), null);

        Map<String, Object> tokenRequest =
                Map.of("requestId", "flash-token-" + suffix, "userId", 10001L, "quantity", 1);
        JsonNode firstToken = ProductionHttpGate.postJson(baseUrl,
                "/api/flash-sales/campaigns/" + campaignId + "/tokens", tokenRequest, null);
        JsonNode secondToken = ProductionHttpGate.postJson(baseUrl,
                "/api/flash-sales/campaigns/" + campaignId + "/tokens", tokenRequest, null);
        JsonNode queued = ProductionHttpGate.postJson(baseUrl, "/api/flash-sales/orders",
                Map.of("requestId", "flash-order-" + suffix, "token", firstToken.path("data").path("token").asText()),
                null);

        assertThat(secondToken.path("data").path("tokenId").asLong())
                .isEqualTo(firstToken.path("data").path("tokenId").asLong());
        assertThat(queued.path("data").path("status").asText()).isEqualTo("QUEUED");
        assertThat(ProductionHttpGate.getJson(baseUrl, "/api/flash-sales/campaigns/" + campaignId + "/stock")
                .path("data").path("queuedStock").asInt()).isEqualTo(1);
    }
}
