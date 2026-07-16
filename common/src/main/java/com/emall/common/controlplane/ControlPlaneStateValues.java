package com.emall.common.controlplane;

import java.util.LinkedHashMap;
import java.util.Map;

final class ControlPlaneStateValues {
    private ControlPlaneStateValues() {
    }

    static String text(Map<String, Object> state, String name) {
        Object value = state.get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(name + " must be a non-blank string");
        }
        return text;
    }

    static String optionalText(Map<String, Object> state, String name, String fallback) {
        Object value = state.get(name);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }

    static long number(Map<String, Object> state, String name) {
        Object value = state.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException(name + " must be numeric");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Map<String, Object> state, String name) {
        Object value = state.get(name);
        if (!(value instanceof Map<?, ?> source)) {
            throw new IllegalArgumentException(name + " must be an object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }
}
