package com.emall.common.event;

import java.util.LinkedHashMap;
import java.util.Map;

public record InventoryReservationEventPayload(String requestId, long skuId, int quantity, Integer bucketNo,
        String status) implements VersionedEventPayload {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    @Override
    public int schemaVersion() {
        return CURRENT_SCHEMA_VERSION;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        payload.put("skuId", skuId);
        payload.put("quantity", quantity);
        payload.put("bucketNo", bucketNo == null ? "" : bucketNo);
        payload.put("status", status);
        return Map.copyOf(payload);
    }

    public static InventoryReservationEventPayload from(OutboxEvent event) {
        EventPayloadValues.requireSupported(event, 1, CURRENT_SCHEMA_VERSION);
        Map<String, Object> payload = event.payload();
        return new InventoryReservationEventPayload(EventPayloadValues.requiredString(payload, "requestId"),
                EventPayloadValues.requiredLong(payload, "skuId"),
                EventPayloadValues.optionalInt(payload, "quantity", 0),
                EventPayloadValues.optionalInteger(payload, "bucketNo"),
                EventPayloadValues.optionalString(payload, "status", "UNKNOWN"));
    }
}
