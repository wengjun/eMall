package com.emall.common.controlplane;

import com.emall.common.idempotency.IdempotencyHeaders;
import com.emall.common.web.OutboundHttpClientFactory;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

public class InfrastructureApiControlPlaneAdapter implements ControlPlaneAdapter {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final ControlPlaneProperties.Infrastructure properties;

    public InfrastructureApiControlPlaneAdapter(OutboundHttpClientFactory clientFactory,
            ControlPlaneProperties.Infrastructure properties) {
        this.restClient = clientFactory.restClient("control-plane-infrastructure", properties.getBaseUrl());
        this.properties = properties;
    }

    @Override
    public ControlPlaneTarget target() {
        return ControlPlaneTarget.INFRASTRUCTURE_API;
    }

    @Override
    public Map<String, Object> captureRollbackState(ControlPlaneOperation operation) {
        Map<String, Object> current = read(operation);
        return current == null ? Map.of("exists", false) : Map.of("exists", true, "state", current);
    }

    @Override
    public void apply(ControlPlaneOperation operation) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("operationId", operation.operationId());
        request.put("action", operation.action());
        request.put("desiredDigest", operation.desiredDigest());
        request.put("desiredState", operation.desiredState());
        put(operation, request);
    }

    @Override
    public ControlPlaneObservation observe(ControlPlaneOperation operation) {
        Map<String, Object> current = read(operation);
        boolean converged = current != null && operation.desiredDigest().equals(current.get("desiredDigest"));
        return new ControlPlaneObservation(converged, current == null ? Map.of("exists", false) : current,
                converged ? "converged" : "infrastructure operator has not converged");
    }

    @Override
    public void rollback(ControlPlaneOperation operation) {
        if (Boolean.TRUE.equals(operation.rollbackState().get("exists"))) {
            put(operation, ControlPlaneStateValues.map(operation.rollbackState(), "state"));
        } else {
            delete(operation);
        }
    }

    @Override
    public ControlPlaneObservation observeRollback(ControlPlaneOperation operation) {
        Map<String, Object> current = read(operation);
        boolean expectedExists = Boolean.TRUE.equals(operation.rollbackState().get("exists"));
        boolean converged = expectedExists == (current != null);
        if (converged && expectedExists) {
            converged = ControlPlaneStateValues.map(operation.rollbackState(), "state").equals(current);
        }
        return new ControlPlaneObservation(converged, current == null ? Map.of("exists", false) : current,
                converged ? "rollback converged" : "infrastructure rollback differs");
    }

    private Map<String, Object> read(ControlPlaneOperation operation) {
        try {
            return restClient.get().uri(path(operation)).headers(this::authorize).retrieve().body(MAP_TYPE);
        } catch (HttpClientErrorException.NotFound exception) {
            return null;
        }
    }

    private void put(ControlPlaneOperation operation, Map<String, Object> request) {
        restClient.put().uri(path(operation)).headers(headers -> {
            authorize(headers);
            headers.set(IdempotencyHeaders.IDEMPOTENCY_KEY, operation.idempotencyKey());
        }).body(request).retrieve().toBodilessEntity();
    }

    private void delete(ControlPlaneOperation operation) {
        try {
            restClient.delete().uri(path(operation)).headers(this::authorize).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
            // Deletion is idempotent.
        }
    }

    private String path(ControlPlaneOperation operation) {
        return "/v1/resources/" + segment(operation.resourceType()) + '/' + segment(operation.resourceId());
    }

    private String segment(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    private void authorize(HttpHeaders headers) {
        if (properties.getBearerToken() != null && !properties.getBearerToken().isBlank()) {
            headers.setBearerAuth(properties.getBearerToken());
        }
    }
}
