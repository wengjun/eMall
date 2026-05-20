package com.emall.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public final class CheckoutSmokeApplication {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final HttpClient httpClient;
    private final String baseUrl;

    private CheckoutSmokeApplication(String baseUrl) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public static void main(String[] args) throws Exception {
        String baseUrl =
                args.length > 0 ? args[0] : System.getenv().getOrDefault("EMALL_BASE_URL", "http://localhost:8080");
        new CheckoutSmokeApplication(baseUrl).run();
    }

    private void run() throws IOException, InterruptedException {
        String suffix = String.valueOf(System.currentTimeMillis());
        String mobile = "155" + suffix.substring(suffix.length() - 8);

        JsonNode user = post("/api/users", Map.of("mobile", mobile, "nickname", "smoke"));

        post("/api/prices", Map.of("skuId", 10001L, "listPrice", BigDecimal.valueOf(3999, 0), "salePrice",
                BigDecimal.valueOf(3799, 0), "currency", "CNY", "active", true));

        post("/api/inventory/10001/stock", Map.of("quantity", 10));

        JsonNode order = post("/api/orders",
                Map.of("requestId", "smoke-order-" + suffix, "userId", user.path("data").path("userId").asLong(),
                        "skuId", 10001L, "quantity", 1, "clientType", "WEB", "deviceId", "java-smoke", "channel",
                        "web-smoke"));
        BigDecimal payableAmount = order.path("data").path("payableAmount").decimalValue();

        JsonNode payment = post("/api/payments",
                Map.of("requestId", "smoke-payment-" + suffix, "orderId", order.path("data").path("orderId").asLong(),
                        "userId", user.path("data").path("userId").asLong(), "amount", payableAmount, "channel",
                        "mock"));

        long paymentId = payment.path("data").path("paymentId").asLong();
        String channelTradeNo = "trade-" + suffix;
        Instant callbackTimestamp = Instant.now();
        String callbackNonce = "smoke-nonce-" + suffix;
        String signature = PaymentCallbackSignature.sign("mock", channelTradeNo, paymentId, payableAmount,
                callbackTimestamp, callbackNonce);
        JsonNode callback = post("/api/payments/" + paymentId + "/callbacks",
                Map.of("channel", "mock", "channelTradeNo", channelTradeNo, "paidAmount", payableAmount, "timestamp",
                        callbackTimestamp, "nonce", callbackNonce, "signature", signature));

        String status = callback.path("data").path("status").asText();
        if (!"SUCCEEDED".equals(status)) {
            throw new IllegalStateException("Payment callback failed with status " + status);
        }

        JsonNode fulfillment = awaitFulfillment(order.path("data").path("orderId").asLong());

        System.out.printf("Smoke checkout succeeded. orderId=%d, paymentId=%d, fulfillmentId=%d%n",
                order.path("data").path("orderId").asLong(), paymentId,
                fulfillment.path("data").path("fulfillmentId").asLong());
    }

    private JsonNode post(String path, Object body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").header("X-Device-Id", "java-smoke")
                .header("X-Client-Channel", "web-smoke")
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body))).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "POST " + path + " failed with HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode json = OBJECT_MAPPER.readTree(response.body());
        if (!json.path("success").asBoolean(false)) {
            throw new IllegalStateException("POST " + path + " failed: " + response.body());
        }
        return json;
    }

    private JsonNode awaitFulfillment(long orderId) throws IOException, InterruptedException {
        String path = "/api/fulfillment/orders/by-order/" + orderId;
        for (int attempt = 0; attempt < 20; attempt++) {
            HttpRequest request =
                    HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode json = OBJECT_MAPPER.readTree(response.body());
                if (json.path("success").asBoolean(false)) {
                    return json;
                }
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("fulfillment order was not created for order " + orderId);
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
