package com.emall.common.trust;

import java.util.Locale;

public record ClientTrustContext(Long accountId, String accessToken, String deviceId, String sourceIp, String channel) {
    public static final String UNKNOWN_DEVICE = "unknown-device";
    public static final String UNKNOWN_IP = "0.0.0.0";
    public static final String DIRECT_CHANNEL = "direct";

    public static ClientTrustContext anonymous() {
        return new ClientTrustContext(null, null, UNKNOWN_DEVICE, UNKNOWN_IP, DIRECT_CHANNEL);
    }

    public static ClientTrustContext of(Long accountId, String accessToken, String deviceId, String sourceIp,
            String channel) {
        return new ClientTrustContext(accountId, normalizeBlank(accessToken), normalizeDevice(deviceId),
                normalizeIp(sourceIp), normalizeChannel(channel));
    }

    public static ClientTrustContext fromBearerHeader(Long accountId, String authorization, String deviceId,
            String sourceIp, String channel) {
        return of(accountId, bearerToken(authorization), deviceId, sourceIp, channel);
    }

    public ClientTrustContext withDefaults(long fallbackAccountId, String fallbackDeviceId, String fallbackChannel) {
        return new ClientTrustContext(accountId == null ? fallbackAccountId : accountId, accessToken,
                deviceId == null || UNKNOWN_DEVICE.equals(deviceId) ? normalizeDevice(fallbackDeviceId) : deviceId,
                sourceIp == null || UNKNOWN_IP.equals(sourceIp) ? UNKNOWN_IP : sourceIp,
                channel == null || DIRECT_CHANNEL.equals(channel) ? normalizeChannel(fallbackChannel) : channel);
    }

    public String subjectId(long fallbackAccountId) {
        return String.valueOf(accountId == null ? fallbackAccountId : accountId);
    }

    private static String bearerToken(String authorization) {
        String normalized = normalizeBlank(authorization);
        if (normalized == null) {
            return null;
        }
        if (normalized.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return normalizeBlank(normalized.substring(7));
        }
        return normalized;
    }

    private static String normalizeBlank(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static String normalizeDevice(String value) {
        String normalized = normalizeBlank(value);
        return normalized == null ? UNKNOWN_DEVICE : normalized;
    }

    private static String normalizeIp(String value) {
        String normalized = normalizeBlank(value);
        if (normalized == null) {
            return UNKNOWN_IP;
        }
        int commaIndex = normalized.indexOf(',');
        return commaIndex < 0 ? normalized : normalized.substring(0, commaIndex).trim();
    }

    private static String normalizeChannel(String value) {
        String normalized = normalizeBlank(value);
        return normalized == null ? DIRECT_CHANNEL : normalized.toLowerCase(Locale.ROOT);
    }
}
