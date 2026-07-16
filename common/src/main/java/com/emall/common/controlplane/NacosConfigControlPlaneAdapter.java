package com.emall.common.controlplane;

import com.emall.common.web.OutboundHttpClientFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

public class NacosConfigControlPlaneAdapter implements ControlPlaneAdapter {
    private static final String CONFIG_PATH = "/nacos/v1/cs/config";

    private final RestClient restClient;
    private final ControlPlaneProperties.Nacos properties;
    private final ObjectMapper objectMapper;

    public NacosConfigControlPlaneAdapter(OutboundHttpClientFactory clientFactory,
            ControlPlaneProperties.Nacos properties, ObjectMapper objectMapper) {
        this.restClient = clientFactory.restClient("control-plane-nacos", properties.getBaseUrl());
        this.properties = properties;
        this.objectMapper = objectMapper.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Override
    public ControlPlaneTarget target() {
        return ControlPlaneTarget.NACOS_CONFIG;
    }

    @Override
    public Map<String, Object> captureRollbackState(ControlPlaneOperation operation) {
        return read(operation.desiredState());
    }

    @Override
    public void apply(ControlPlaneOperation operation) {
        publish(operation.desiredState(), content(operation.desiredState().get("content")));
    }

    @Override
    public ControlPlaneObservation observe(ControlPlaneOperation operation) {
        Map<String, Object> actual = read(operation.desiredState());
        String expected = content(operation.desiredState().get("content"));
        boolean converged = Boolean.TRUE.equals(actual.get("exists"))
                && jsonEquivalent(expected, String.valueOf(actual.get("content")));
        return new ControlPlaneObservation(converged, actual, converged ? "converged" : "Nacos content differs");
    }

    @Override
    public void rollback(ControlPlaneOperation operation) {
        Map<String, Object> rollback = operation.rollbackState();
        if (Boolean.TRUE.equals(rollback.get("exists"))) {
            publish(operation.desiredState(), String.valueOf(rollback.get("content")));
        } else {
            delete(operation.desiredState());
        }
    }

    @Override
    public ControlPlaneObservation observeRollback(ControlPlaneOperation operation) {
        Map<String, Object> actual = read(operation.desiredState());
        Map<String, Object> rollback = operation.rollbackState();
        boolean expectedExists = Boolean.TRUE.equals(rollback.get("exists"));
        boolean converged = expectedExists == Boolean.TRUE.equals(actual.get("exists"));
        if (converged && expectedExists) {
            converged = jsonEquivalent(String.valueOf(rollback.get("content")), String.valueOf(actual.get("content")));
        }
        return new ControlPlaneObservation(converged, actual, converged ? "rollback converged" : "rollback differs");
    }

    private Map<String, Object> read(Map<String, Object> state) {
        try {
            String content = restClient.get().uri(builder -> configUri(builder, state)).retrieve().body(String.class);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("exists", true);
            result.put("content", content == null ? "" : content);
            return result;
        } catch (HttpClientErrorException.NotFound exception) {
            return Map.of("exists", false);
        }
    }

    private void publish(Map<String, Object> state, String content) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("dataId", ControlPlaneStateValues.text(state, "dataId"));
        form.add("group", ControlPlaneStateValues.optionalText(state, "group", properties.getGroup()));
        form.add("tenant", ControlPlaneStateValues.optionalText(state, "namespace", properties.getNamespace()));
        form.add("type", "json");
        form.add("content", content);
        addAccessToken(form);
        String response = restClient.post().uri(CONFIG_PATH).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(String.class);
        if (!"true".equalsIgnoreCase(response)) {
            throw new IllegalStateException("Nacos rejected config publication");
        }
    }

    private void delete(Map<String, Object> state) {
        restClient.delete().uri(builder -> configUri(builder, state)).retrieve().toBodilessEntity();
    }

    private java.net.URI configUri(org.springframework.web.util.UriBuilder builder, Map<String, Object> state) {
        builder.path(CONFIG_PATH).queryParam("dataId", ControlPlaneStateValues.text(state, "dataId"))
                .queryParam("group", ControlPlaneStateValues.optionalText(state, "group", properties.getGroup()))
                .queryParam("tenant",
                        ControlPlaneStateValues.optionalText(state, "namespace", properties.getNamespace()));
        if (properties.getAccessToken() != null && !properties.getAccessToken().isBlank()) {
            builder.queryParam("accessToken", properties.getAccessToken());
        }
        return builder.build();
    }

    private void addAccessToken(MultiValueMap<String, String> form) {
        if (properties.getAccessToken() != null && !properties.getAccessToken().isBlank()) {
            form.add("accessToken", properties.getAccessToken());
        }
    }

    private String content(Object value) {
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Nacos content is not JSON serializable", exception);
        }
    }

    private boolean jsonEquivalent(String first, String second) {
        try {
            return objectMapper.readTree(first).equals(objectMapper.readTree(second));
        } catch (JsonProcessingException exception) {
            return first.equals(second);
        }
    }
}
