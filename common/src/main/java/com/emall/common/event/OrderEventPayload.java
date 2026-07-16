package com.emall.common.event;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public record OrderEventPayload(long orderId, long userId, long skuId, int quantity, String clientType, String deviceId,
        String channel, BigDecimal unitPrice, BigDecimal subtotalAmount, BigDecimal discountAmount,
        BigDecimal payableAmount, String currency, long priceVersion, String couponId, String inventoryReservationId,
        String status) implements VersionedEventPayload {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    @Override
    public int schemaVersion() {
        return CURRENT_SCHEMA_VERSION;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("userId", userId);
        payload.put("skuId", skuId);
        payload.put("quantity", quantity);
        payload.put("clientType", clientType);
        payload.put("deviceId", deviceId);
        payload.put("channel", channel);
        payload.put("unitPrice", unitPrice);
        payload.put("subtotalAmount", subtotalAmount);
        payload.put("discountAmount", discountAmount);
        payload.put("payableAmount", payableAmount);
        payload.put("currency", currency);
        payload.put("priceVersion", priceVersion);
        payload.put("couponId", couponId == null ? "" : couponId);
        payload.put("inventoryReservationId", inventoryReservationId == null ? "" : inventoryReservationId);
        payload.put("status", status);
        return Map.copyOf(payload);
    }

    public static OrderEventPayload from(OutboxEvent event) {
        EventPayloadValues.requireSupported(event, 1, CURRENT_SCHEMA_VERSION);
        Map<String, Object> payload = event.payload();
        return new OrderEventPayload(EventPayloadValues.requiredLong(payload, "orderId"),
                EventPayloadValues.optionalLong(payload, "userId", 0L),
                EventPayloadValues.optionalLong(payload, "skuId", 0L),
                EventPayloadValues.optionalInt(payload, "quantity", 0),
                EventPayloadValues.optionalString(payload, "clientType", "UNKNOWN"),
                EventPayloadValues.optionalString(payload, "deviceId", "unknown"),
                EventPayloadValues.optionalString(payload, "channel", "unknown"),
                EventPayloadValues.optionalDecimal(payload, "unitPrice", BigDecimal.ZERO),
                EventPayloadValues.optionalDecimal(payload, "subtotalAmount", BigDecimal.ZERO),
                EventPayloadValues.optionalDecimal(payload, "discountAmount", BigDecimal.ZERO),
                EventPayloadValues.optionalDecimal(payload, "payableAmount", BigDecimal.ZERO),
                EventPayloadValues.optionalString(payload, "currency", "CNY"),
                EventPayloadValues.optionalLong(payload, "priceVersion", 0L),
                EventPayloadValues.optionalString(payload, "couponId", ""),
                EventPayloadValues.optionalString(payload, "inventoryReservationId", ""),
                EventPayloadValues.optionalString(payload, "status", "UNKNOWN"));
    }
}
