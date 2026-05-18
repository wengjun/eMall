package com.emall.order.domain;

import java.util.Locale;

public record OrderClientContext(OrderClientType clientType, String deviceId, String channel) {
    public static final String UNKNOWN_DEVICE = "unknown-device";
    public static final String DIRECT_CHANNEL = "direct";

    public static OrderClientContext webDefault() {
        return of(OrderClientType.WEB, UNKNOWN_DEVICE, DIRECT_CHANNEL);
    }

    public static OrderClientContext of(OrderClientType clientType, String deviceId, String channel) {
        return new OrderClientContext(OrderClientType.defaultIfNull(clientType), normalizeDevice(deviceId),
                normalizeChannel(channel));
    }

    private static String normalizeDevice(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? UNKNOWN_DEVICE : normalized;
    }

    private static String normalizeChannel(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? DIRECT_CHANNEL : normalized;
    }
}
