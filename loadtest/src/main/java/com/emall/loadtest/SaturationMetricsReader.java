package com.emall.loadtest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class SaturationMetricsReader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Double>> TYPE = new TypeReference<>() {
    };

    private SaturationMetricsReader() {
    }

    static Map<String, Double> read(Path path) {
        if (path == null) {
            return Map.of();
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("saturation metrics file does not exist: " + path);
        }
        try {
            Map<String, Double> values = OBJECT_MAPPER.readValue(path.toFile(), TYPE);
            Map<String, Double> validated = new LinkedHashMap<>();
            values.forEach((name, value) -> {
                if (name == null || name.isBlank() || value == null || !Double.isFinite(value) || value < 0.0) {
                    throw new IllegalArgumentException("invalid saturation metric: " + name);
                }
                if (name.endsWith(".utilization") && value > 1.0) {
                    throw new IllegalArgumentException("utilization metric must be in [0, 1]: " + name);
                }
                validated.put(name, value);
            });
            return Map.copyOf(validated);
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to read saturation metrics", ex);
        }
    }
}
