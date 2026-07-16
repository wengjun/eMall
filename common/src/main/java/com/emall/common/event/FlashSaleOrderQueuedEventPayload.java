package com.emall.common.event;

import java.util.Map;

public record FlashSaleOrderQueuedEventPayload(long requestId, long campaignId, long userId, long skuId, int quantity,
        String status) implements VersionedEventPayload {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    @Override
    public int schemaVersion() {
        return CURRENT_SCHEMA_VERSION;
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of("requestId", requestId, "campaignId", campaignId, "userId", userId, "skuId", skuId, "quantity",
                quantity, "status", status);
    }

    public static FlashSaleOrderQueuedEventPayload from(OutboxEvent event) {
        EventPayloadValues.requireSupported(event, 1, CURRENT_SCHEMA_VERSION);
        Map<String, Object> payload = event.payload();
        return new FlashSaleOrderQueuedEventPayload(EventPayloadValues.requiredLong(payload, "requestId"),
                EventPayloadValues.requiredLong(payload, "campaignId"),
                EventPayloadValues.requiredLong(payload, "userId"), EventPayloadValues.requiredLong(payload, "skuId"),
                EventPayloadValues.optionalInt(payload, "quantity", 1),
                EventPayloadValues.optionalString(payload, "status", "QUEUED"));
    }
}
