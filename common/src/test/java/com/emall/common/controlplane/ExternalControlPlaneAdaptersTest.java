package com.emall.common.controlplane;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.web.OutboundHttpClientFactory;
import com.emall.common.web.OutboundHttpClientProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ExternalControlPlaneAdaptersTest {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OutboundHttpClientFactory clientFactory = clientFactory();
    private HttpServer server;

    @AfterEach
    void closeResources() {
        if (server != null) {
            server.stop(0);
        }
        clientFactory.close();
    }

    @Test
    void publishesAndReadsBackNacosConfiguration() throws IOException {
        AtomicReference<String> content = new AtomicReference<>();
        server = server(exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                content.set(form(exchange).get("content"));
                respond(exchange, 200, "true", "text/plain");
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                content.set(null);
                respond(exchange, 200, "true", "text/plain");
            } else if (content.get() == null) {
                respond(exchange, 404, "missing", "text/plain");
            } else {
                respond(exchange, 200, content.get(), "application/json");
            }
        });
        ControlPlaneProperties.Nacos properties = new ControlPlaneProperties.Nacos();
        properties.setBaseUrl(baseUrl());
        NacosConfigControlPlaneAdapter adapter =
                new NacosConfigControlPlaneAdapter(clientFactory, properties, objectMapper);
        ControlPlaneOperation operation = operation(ControlPlaneCommands.nacosConfig("nacos-1", "traffic", "sync-rules",
                "sentinel-rules", "global", "rules.json", "CONTROL", "public", Map.of("enabled", true)));

        assertThat(adapter.captureRollbackState(operation)).containsEntry("exists", false);
        adapter.apply(operation);

        assertThat(adapter.observe(operation).converged()).isTrue();
        assertThat(content.get()).contains("enabled");
    }

    @Test
    void serverSideAppliesAndReadsBackKubernetesResource() throws IOException {
        AtomicReference<String> resource = new AtomicReference<>();
        server = server(exchange -> {
            if ("PATCH".equals(exchange.getRequestMethod())) {
                resource.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                respond(exchange, 200, resource.get(), "application/json");
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                resource.set(null);
                respond(exchange, 200, "{}", "application/json");
            } else if (resource.get() == null) {
                respond(exchange, 404, "{}", "application/json");
            } else {
                respond(exchange, 200, resource.get(), "application/json");
            }
        });
        ControlPlaneProperties.Kubernetes properties = new ControlPlaneProperties.Kubernetes();
        properties.setBaseUrl(baseUrl());
        properties.setBearerTokenFile("");
        properties.setCaCertificateFile("");
        KubernetesResourceControlPlaneAdapter adapter =
                new KubernetesResourceControlPlaneAdapter(clientFactory, properties, objectMapper);
        Map<String, Object> manifest = Map.of("apiVersion", "v1", "kind", "ConfigMap", "metadata",
                Map.of("name", "routing"), "data", Map.of("active", "east"));
        ControlPlaneOperation operation = operation(ControlPlaneCommands.kubernetesResource("kubernetes-1", "traffic",
                "sync-routing", "configmap", "routing", "v1", "configmaps", "emall", "routing", manifest));

        assertThat(adapter.captureRollbackState(operation)).containsEntry("exists", false);
        adapter.apply(operation);

        assertThat(adapter.observe(operation).converged()).isTrue();
        assertThat(resource.get()).contains("control-plane.emall.com/desired-digest");
    }

    @Test
    void appliesAndReadsBackInfrastructureOperatorState() throws IOException {
        AtomicReference<Map<String, Object>> state = new AtomicReference<>();
        server = server(exchange -> {
            if ("PUT".equals(exchange.getRequestMethod())) {
                Map<String, Object> request = objectMapper.readValue(exchange.getRequestBody(), MAP_TYPE);
                Map<String, Object> observed = new LinkedHashMap<>();
                observed.put("desiredDigest", request.get("desiredDigest"));
                observed.put("phase", "READY");
                state.set(observed);
                respond(exchange, 200, "{}", "application/json");
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                state.set(null);
                respond(exchange, 204, "", "application/json");
            } else if (state.get() == null) {
                respond(exchange, 404, "{}", "application/json");
            } else {
                respond(exchange, 200, objectMapper.writeValueAsString(state.get()), "application/json");
            }
        });
        ControlPlaneProperties.Infrastructure properties = new ControlPlaneProperties.Infrastructure();
        properties.setBaseUrl(baseUrl());
        InfrastructureApiControlPlaneAdapter adapter =
                new InfrastructureApiControlPlaneAdapter(clientFactory, properties);
        ControlPlaneOperation operation = operation(ControlPlaneCommands.infrastructure("infra-1", "platform-ops",
                "execute-backup", "database-backup", "7", Map.of("retentionDays", 30)));

        assertThat(adapter.captureRollbackState(operation)).containsEntry("exists", false);
        adapter.apply(operation);

        assertThat(adapter.observe(operation).converged()).isTrue();
        assertThat(state.get()).containsEntry("phase", "READY");
    }

    private ControlPlaneOperation operation(ControlPlaneCommand command) {
        ControlPlaneProperties properties = new ControlPlaneProperties();
        ControlPlaneCommandService service = new ControlPlaneCommandService(new InMemoryControlPlaneOperationStore(),
                properties, objectMapper, Clock.systemUTC());
        return service.submit(command);
    }

    private HttpServer server(ExchangeHandler handler) throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        created.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        created.start();
        return created;
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private Map<String, String> form(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts.length == 1 ? "" : parts[1], StandardCharsets.UTF_8));
        }
        return values;
    }

    private void respond(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
        if (status != 204) {
            exchange.getResponseBody().write(bytes);
        }
    }

    private OutboundHttpClientFactory clientFactory() {
        OutboundHttpClientProperties properties = new OutboundHttpClientProperties();
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(2));
        properties.setMaxConnections(4);
        properties.setBulkheadMaxConcurrent(4);
        properties.setMaxAttempts(1);
        return new OutboundHttpClientFactory(properties);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
