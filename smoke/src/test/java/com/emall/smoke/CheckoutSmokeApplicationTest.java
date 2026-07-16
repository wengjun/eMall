package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CheckoutSmokeApplicationTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldTrimTrailingSlashFromBaseUrl() throws Exception {
        Method method = CheckoutSmokeApplication.class.getDeclaredMethod("trimTrailingSlash", String.class);
        method.setAccessible(true);

        assertThat(method.invoke(null, "http://localhost:8080/")).isEqualTo("http://localhost:8080");
        assertThat(method.invoke(null, "http://localhost:8080")).isEqualTo("http://localhost:8080");
    }

    @Test
    void shouldCompleteAuthenticatedCheckoutAgainstHttpGateway() throws Exception {
        Map<String, String> authorizationByPath = new ConcurrentHashMap<>();
        Map<String, String> idempotencyByPath = new ConcurrentHashMap<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> respond(exchange, authorizationByPath, idempotencyByPath));
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        new CheckoutSmokeApplication(baseUrl, "setup-token").run();

        assertThat(authorizationByPath.get("/api/identity/registrations")).isEmpty();
        assertThat(idempotencyByPath.get("/api/identity/registrations")).startsWith("smoke-registration-");
        assertThat(authorizationByPath.keySet()).anyMatch(path -> path.startsWith("/api/identity/registrations/"));
        assertThat(authorizationByPath.get("/api/identity/sessions")).isEmpty();
        assertThat(authorizationByPath.get("/api/users/70001")).isEqualTo("Bearer shopper-token");
        assertThat(authorizationByPath.get("/api/prices")).isEqualTo("Bearer setup-token");
        assertThat(authorizationByPath.get("/api/inventory/10001/stock")).isEqualTo("Bearer setup-token");
        assertThat(authorizationByPath.get("/api/orders")).isEqualTo("Bearer shopper-token");
        assertThat(authorizationByPath.get("/api/payments")).isEqualTo("Bearer shopper-token");
        assertThat(authorizationByPath.get("/api/payments/90001/callbacks")).isEmpty();
        assertThat(authorizationByPath.get("/api/fulfillment/orders/by-order/80001")).isEqualTo("Bearer setup-token");
    }

    @Test
    void shouldCreateStablePaymentCallbackSignature() {
        Instant timestamp = Instant.parse("2026-07-12T00:00:00Z");

        String first = PaymentCallbackSignature.sign("secret", "mock", "trade-1", 90001L, new BigDecimal("3799.00"),
                timestamp, "nonce-1");
        String second = PaymentCallbackSignature.sign("secret", "mock", "trade-1", 90001L, new BigDecimal("3799"),
                timestamp, "nonce-1");

        assertThat(first).isEqualTo(second).isNotBlank();
        assertThat(PaymentCallbackSignature.sign("secret", "mock", "trade-2", 90001L, new BigDecimal("3799"), timestamp,
                "nonce-1")).isNotEqualTo(first);
    }

    private void respond(HttpExchange exchange, Map<String, String> authorizationByPath,
            Map<String, String> idempotencyByPath) throws IOException {
        String path = exchange.getRequestURI().getPath();
        authorizationByPath.put(path, header(exchange, "Authorization"));
        idempotencyByPath.put(path, header(exchange, "Idempotency-Key"));
        String response = switch (path) {
            case "/api/identity/registrations" -> "{\"success\":true,\"data\":{\"accountId\":70001}}";
            case "/api/identity/sessions" -> "{\"success\":true,\"data\":{\"accessToken\":\"shopper-token\"}}";
            case "/api/users/70001" -> "{\"success\":true,\"data\":{\"userId\":70001}}";
            case "/api/orders" -> "{\"success\":true,\"data\":{\"orderId\":80001,\"payableAmount\":3799}}";
            case "/api/payments" -> "{\"success\":true,\"data\":{\"paymentId\":90001}}";
            case "/api/payments/90001/callbacks" -> "{\"success\":true,\"data\":{\"status\":\"SUCCEEDED\"}}";
            case "/api/fulfillment/orders/by-order/80001" -> "{\"success\":true,\"data\":{\"fulfillmentId\":60001}}";
            default -> path.startsWith("/api/identity/registrations/")
                    ? "{\"success\":true,\"data\":{\"accountId\":70001,\"accountStatus\":\"ACTIVE\"}}"
                    : "{\"success\":true,\"data\":{}}";
        };
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private String header(HttpExchange exchange, String name) {
        String value = exchange.getRequestHeaders().getFirst(name);
        return value == null ? "" : value;
    }
}
