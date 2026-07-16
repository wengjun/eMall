package com.emall.common.event;

import java.math.BigDecimal;
import java.util.Map;

final class EventPayloadValues {
    private EventPayloadValues() {
    }

    static void requireSupported(OutboxEvent event, int minimum, int maximum) {
        if (event.schemaVersion() < minimum || event.schemaVersion() > maximum) {
            throw new IllegalArgumentException(
                    "unsupported " + event.eventType() + " schema version: " + event.schemaVersion());
        }
    }

    static long requiredLong(Map<String, Object> payload, String field) {
        Object value = required(payload, field);
        try {
            return new BigDecimal(String.valueOf(value)).longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalid(field, exception);
        }
    }

    static long optionalLong(Map<String, Object> payload, String field, long fallback) {
        return present(payload, field) ? requiredLong(payload, field) : fallback;
    }

    static int optionalInt(Map<String, Object> payload, String field, int fallback) {
        long value = optionalLong(payload, field, fallback);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw invalid(field, null);
        }
        return (int) value;
    }

    static Integer optionalInteger(Map<String, Object> payload, String field) {
        return present(payload, field) ? optionalInt(payload, field, 0) : null;
    }

    static String requiredString(Map<String, Object> payload, String field) {
        Object rawValue = required(payload, field);
        if (!(rawValue instanceof String value) || value.isBlank()) {
            throw invalid(field, null);
        }
        return value;
    }

    static String optionalString(Map<String, Object> payload, String field, String fallback) {
        return present(payload, field) ? requiredString(payload, field) : fallback;
    }

    static BigDecimal optionalDecimal(Map<String, Object> payload, String field, BigDecimal fallback) {
        if (!present(payload, field)) {
            return fallback;
        }
        Object value = payload.get(field);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw invalid(field, exception);
        }
    }

    static BigDecimal requiredDecimal(Map<String, Object> payload, String field) {
        required(payload, field);
        return optionalDecimal(payload, field, BigDecimal.ZERO);
    }

    static boolean optionalBoolean(Map<String, Object> payload, String field, boolean fallback) {
        if (!present(payload, field)) {
            return fallback;
        }
        Object value = payload.get(field);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text))) {
            return Boolean.parseBoolean(text);
        }
        throw invalid(field, null);
    }

    static boolean requiredBoolean(Map<String, Object> payload, String field) {
        required(payload, field);
        return optionalBoolean(payload, field, false);
    }

    private static Object required(Map<String, Object> payload, String field) {
        if (!present(payload, field)) {
            throw invalid(field, null);
        }
        return payload.get(field);
    }

    private static boolean present(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        return value != null && !String.valueOf(value).isBlank() && !"null".equalsIgnoreCase(String.valueOf(value));
    }

    private static IllegalArgumentException invalid(String field, RuntimeException cause) {
        return new IllegalArgumentException("event payload field is missing or invalid: " + field, cause);
    }
}
