package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IdempotencyContractIT {
    @Test
    void shouldKeepOrderPaymentAndCallbackIdempotentAgainstRunningGateway() throws Exception {
        ProductionHttpGate.assumeEnabled("EMALL_RUN_IDEMPOTENCY_IT");
        String baseUrl = ProductionHttpGate.envOrDefault("EMALL_BASE_URL", "http://localhost:8080");
        String suffix = String.valueOf(System.currentTimeMillis());
        long skuId = Long.parseLong("8" + suffix.substring(suffix.length() - 8));

        long userId = ProductionHttpGate.postJson(baseUrl, "/api/users",
                Map.of("mobile", "177" + suffix.substring(suffix.length() - 8), "nickname", "idempotency"), null)
                .path("data").path("userId").asLong();
        ProductionHttpGate.postJson(baseUrl, "/api/prices",
                Map.of("skuId", skuId, "listPrice", BigDecimal.valueOf(2999, 0), "salePrice",
                        BigDecimal.valueOf(2599, 0), "currency", "CNY", "active", true),
                null);
        ProductionHttpGate.postJson(baseUrl, "/api/inventory/" + skuId + "/stock", Map.of("quantity", 5), null);

        Map<String, Object> orderRequest =
                Map.of("requestId", "idem-order-" + suffix, "userId", userId, "skuId", skuId, "quantity", 1,
                        "clientType", "APP", "deviceId", "app-device-" + suffix, "channel", "android-app");
        JsonNode firstOrder = ProductionHttpGate.postJson(baseUrl, "/api/orders", orderRequest, null);
        JsonNode secondOrder = ProductionHttpGate.postJson(baseUrl, "/api/orders", orderRequest, null);
        assertThat(firstOrder.path("data").path("clientType").asText()).isEqualTo("APP");
        assertThat(secondOrder.path("data").path("clientType").asText()).isEqualTo("APP");
        assertThat(firstOrder.path("data").path("deviceId").asText()).startsWith("app-device-");
        assertThat(firstOrder.path("data").path("channel").asText()).isEqualTo("android-app");
        assertThat(secondOrder.path("data").path("orderId").asLong())
                .isEqualTo(firstOrder.path("data").path("orderId").asLong());

        Map<String, Object> paymentRequest = Map.of("requestId", "idem-payment-" + suffix, "orderId",
                firstOrder.path("data").path("orderId").asLong(), "userId", userId, "amount",
                firstOrder.path("data").path("payableAmount").decimalValue(), "channel", "mock");
        JsonNode firstPayment = ProductionHttpGate.postJson(baseUrl, "/api/payments", paymentRequest, null);
        JsonNode secondPayment = ProductionHttpGate.postJson(baseUrl, "/api/payments", paymentRequest, null);
        assertThat(secondPayment.path("data").path("paymentId").asLong())
                .isEqualTo(firstPayment.path("data").path("paymentId").asLong());

        long paymentId = firstPayment.path("data").path("paymentId").asLong();
        Map<String, Object> callbackRequest = Map.of("channelTradeNo", "idem-trade-" + suffix, "paidAmount",
                firstOrder.path("data").path("payableAmount").decimalValue());
        JsonNode firstCallback = ProductionHttpGate.postJson(baseUrl, "/api/payments/" + paymentId + "/callbacks",
                callbackRequest, null);
        JsonNode secondCallback = ProductionHttpGate.postJson(baseUrl, "/api/payments/" + paymentId + "/callbacks",
                callbackRequest, null);
        assertThat(secondCallback.path("data").path("status").asText())
                .isEqualTo(firstCallback.path("data").path("status").asText()).isEqualTo("SUCCEEDED");
    }
}
