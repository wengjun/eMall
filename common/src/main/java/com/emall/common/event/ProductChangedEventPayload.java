package com.emall.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ProductChangedEventPayload(long skuId, long spuId, String title, String category, BigDecimal price,
        String status, boolean saleable, Instant updatedAt) implements VersionedEventPayload {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    @Override
    public int schemaVersion() {
        return CURRENT_SCHEMA_VERSION;
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of("skuId", skuId, "spuId", spuId, "title", title, "category", category, "price", price, "status",
                status, "saleable", saleable, "updatedAt", updatedAt.toString());
    }

    public static ProductChangedEventPayload from(OutboxEvent event) {
        EventPayloadValues.requireSupported(event, 1, CURRENT_SCHEMA_VERSION);
        Map<String, Object> payload = event.payload();
        Instant updatedAt =
                parseInstant(EventPayloadValues.optionalString(payload, "updatedAt", event.occurredAt().toString()));
        return new ProductChangedEventPayload(EventPayloadValues.requiredLong(payload, "skuId"),
                EventPayloadValues.optionalLong(payload, "spuId", 0L),
                EventPayloadValues.requiredString(payload, "title"),
                EventPayloadValues.requiredString(payload, "category"),
                EventPayloadValues.requiredDecimal(payload, "price"),
                EventPayloadValues.optionalString(payload, "status", "UNKNOWN"),
                EventPayloadValues.requiredBoolean(payload, "saleable"), updatedAt);
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("event payload field is invalid: updatedAt", exception);
        }
    }
}
