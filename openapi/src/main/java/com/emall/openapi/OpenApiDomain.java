package com.emall.openapi;

import java.time.Instant;
import java.time.LocalDate;

enum WebhookDeliveryStatus {
    PENDING,
    DELIVERED,
    FAILED
}

record OpenApiApp(
        long appId,
        long merchantId,
        String appKey,
        String secretHash,
        String name,
        String scopes,
        int dailyQuota,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

record AppRegistration(
        OpenApiApp app,
        String appSecret
) {
}

record ApiSignatureVerification(
        String appKey,
        String requestPath,
        String nonce,
        long timestamp,
        boolean valid,
        String reason
) {
}

record ApiQuotaUsage(
        String appKey,
        LocalDate usageDate,
        int usedCount,
        int dailyQuota,
        boolean allowed
) {
}

record WebhookSubscription(
        long subscriptionId,
        long appId,
        String eventType,
        String targetUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

record WebhookDelivery(
        long deliveryId,
        long subscriptionId,
        String eventId,
        WebhookDeliveryStatus status,
        int retryCount,
        Instant createdAt,
        Instant updatedAt
) {
    WebhookDelivery mark(WebhookDeliveryStatus nextStatus) {
        return new WebhookDelivery(deliveryId, subscriptionId, eventId, nextStatus, retryCount + 1, createdAt,
                Instant.now());
    }
}
