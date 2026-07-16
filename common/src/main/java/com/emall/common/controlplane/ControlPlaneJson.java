package com.emall.common.controlplane;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

final class ControlPlaneJson {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    ControlPlaneJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    String write(Map<String, Object> state) {
        if (state == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("control-plane state is not JSON serializable", exception);
        }
    }

    Map<String, Object> read(String state) {
        if (state == null) {
            return null;
        }
        try {
            return objectMapper.readValue(state, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("stored control-plane state is invalid", exception);
        }
    }

    String digest(Map<String, Object> state) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(write(state).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
