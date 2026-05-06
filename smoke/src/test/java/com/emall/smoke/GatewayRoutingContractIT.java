package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GatewayRoutingContractIT {
    @Test
    void shouldRouteCoreCommerceApisThroughGateway() throws Exception {
        ProductionHttpGate.assumeEnabled("EMALL_RUN_GATEWAY_CONTRACT_IT");
        String baseUrl = ProductionHttpGate.envOrDefault("EMALL_BASE_URL", "http://localhost:8080");
        String suffix = String.valueOf(System.currentTimeMillis());

        long userId = ProductionHttpGate.postJson(baseUrl, "/api/users", Map.of(
                "mobile", "166" + suffix.substring(suffix.length() - 8),
                "nickname", "gateway-contract"), null)
                .path("data")
                .path("userId")
                .asLong();
        long skuId = ProductionHttpGate.postJson(baseUrl, "/api/products", Map.of(
                "spuId", Long.parseLong("7" + suffix.substring(suffix.length() - 8)),
                "title", "gateway contract sku " + suffix,
                "category", "contract",
                "price", BigDecimal.valueOf(1999, 0)), null)
                .path("data")
                .path("skuId")
                .asLong();

        ProductionHttpGate.postJson(baseUrl, "/api/prices", Map.of(
                "skuId", skuId,
                "listPrice", BigDecimal.valueOf(1999, 0),
                "salePrice", BigDecimal.valueOf(1799, 0),
                "currency", "CNY",
                "active", true), null);
        ProductionHttpGate.postJson(baseUrl, "/api/inventory/" + skuId + "/stock",
                Map.of("quantity", 5), null);

        assertThat(ProductionHttpGate.getJson(baseUrl, "/api/products/" + skuId)
                .path("data")
                .path("skuId")
                .asLong()).isEqualTo(skuId);
        assertThat(ProductionHttpGate.postJson(baseUrl, "/api/prices/quotes", Map.of(
                "skuId", skuId,
                "quantity", 2), null)
                .path("data")
                .path("subtotal")
                .decimalValue()).isEqualByComparingTo(BigDecimal.valueOf(3598, 0));
        assertThat(ProductionHttpGate.postJson(baseUrl, "/api/marketing/quotes", Map.of(
                "userId", userId,
                "orderAmount", BigDecimal.valueOf(3598, 0)), null)
                .path("data")
                .path("payableAmount")
                .decimalValue()).isPositive();
    }
}
