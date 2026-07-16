package com.emall.common.event;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentEventPayload(long paymentId, long orderId, long userId, BigDecimal amount,
        String status) implements VersionedEventPayload {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    @Override
    public int schemaVersion() {
        return CURRENT_SCHEMA_VERSION;
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of("paymentId", paymentId, "orderId", orderId, "userId", userId, "amount", amount, "status", status);
    }

    public static PaymentEventPayload from(OutboxEvent event) {
        EventPayloadValues.requireSupported(event, 1, CURRENT_SCHEMA_VERSION);
        Map<String, Object> payload = event.payload();
        return new PaymentEventPayload(EventPayloadValues.requiredLong(payload, "paymentId"),
                EventPayloadValues.requiredLong(payload, "orderId"),
                EventPayloadValues.optionalLong(payload, "userId", 0L),
                EventPayloadValues.optionalDecimal(payload, "amount", BigDecimal.ZERO),
                EventPayloadValues.optionalString(payload, "status", "UNKNOWN"));
    }
}
