package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

final class ProductionHttpGate {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private ProductionHttpGate() {
    }

    static void assumeEnabled(String envName) {
        assumeTrue(Boolean.parseBoolean(System.getenv(envName)),
                () -> "Set " + envName + "=true to run this production integration test.");
    }

    static String requireEnv(String envName) {
        String value = System.getenv(envName);
        assumeTrue(value != null && !value.isBlank(),
                () -> "Set " + envName + " to run this production integration test.");
        return value;
    }

    static String envOrDefault(String envName, String defaultValue) {
        String value = System.getenv(envName);
        return trimTrailingSlash(value == null || value.isBlank() ? defaultValue : value);
    }

    static JsonNode post(String baseUrl, String path, String token) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder(baseUrl, path, token).POST(HttpRequest.BodyPublishers.noBody()).build();
        return sendApiRequest(request, "POST " + path);
    }

    static JsonNode postJson(String baseUrl, String path, Object body, String token)
            throws IOException, InterruptedException {
        return postJson(baseUrl, path, body, token, Map.of());
    }

    static JsonNode postJson(String baseUrl, String path, Object body, String token, Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = requestBuilder(baseUrl, path, token).header("Content-Type", "application/json");
        headers.forEach(builder::header);
        HttpRequest request =
                builder.POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body))).build();
        return sendApiRequest(request, "POST " + path);
    }

    static long registerShopper(String baseUrl, String mobile, String nickname) throws Exception {
        String registrationId = "production-it-registration-" + System.currentTimeMillis() + "-" + mobile;
        JsonNode response = postJson(baseUrl, "/api/identity/registrations",
                Map.of("mobile", mobile, "displayName", nickname, "password", "ProductionIT12345"), null,
                Map.of("Idempotency-Key", registrationId));
        long accountId = response.path("data").path("accountId").asLong();
        for (int attempt = 0; attempt < 40; attempt++) {
            JsonNode status = getJson(baseUrl, "/api/identity/registrations/" + registrationId).path("data");
            assertThat(status.path("accountId").asLong()).isEqualTo(accountId);
            if ("ACTIVE".equals(status.path("accountStatus").asText())) {
                return accountId;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("registration did not become active: " + registrationId);
    }

    static JsonNode patchJson(String baseUrl, String path, Object body, String token)
            throws IOException, InterruptedException {
        HttpRequest request = requestBuilder(baseUrl, path, token).header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body))).build();
        return sendApiRequest(request, "PATCH " + path);
    }

    static JsonNode getJson(String baseUrl, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(baseUrl) + path))
                .timeout(Duration.ofSeconds(10)).GET().build();
        return sendApiRequest(request, "GET " + path);
    }

    static String getText(String baseUrl, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(baseUrl) + path))
                .timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("GET %s status", path).isBetween(200, 299);
        return response.body();
    }

    static int postStatus(String baseUrl, String path, String token) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder(baseUrl, path, token).POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    private static JsonNode sendApiRequest(HttpRequest request, String operation)
            throws IOException, InterruptedException {
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("%s status", operation).isBetween(200, 299);
        JsonNode json = OBJECT_MAPPER.readTree(response.body());
        assertThat(json.path("success").asBoolean(false)).as("%s response body", operation).isTrue();
        return json;
    }

    private static HttpRequest.Builder requestBuilder(String baseUrl, String path, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(trimTrailingSlash(baseUrl) + path))
                .timeout(Duration.ofSeconds(15)).header("X-Operator", "production-it")
                .header("X-Trace-Id", "production-it-" + System.currentTimeMillis());
        if (token != null && !token.isBlank()) {
            builder.header("X-Internal-Token", token);
        }
        return builder;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
