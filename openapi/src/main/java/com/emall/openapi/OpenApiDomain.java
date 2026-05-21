package com.emall.openapi;

import java.time.Instant;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

enum WebhookDeliveryStatus {
    PENDING,
    DELIVERED,
    FAILED
}

@TableName("openapi_app")
record OpenApiApp(@TableId(value = "app_id", type = IdType.INPUT) long appId, long merchantId, String appKey,
        String secretHash, String secretCiphertext, String name, String scopes, int dailyQuota, boolean active,
        Instant createdAt, Instant updatedAt) {
}

record AppRegistration(OpenApiApp app, String appSecret) {
}

record ApiSignatureVerification(String appKey, String requestPath, String nonce, long timestamp, boolean valid,
        String reason) {
}

@TableName("openapi_quota_usage")
record ApiQuotaUsage(@TableId(value = "app_key", type = IdType.INPUT) String appKey, LocalDate usageDate,
        int usedCount, int dailyQuota, boolean allowed) {
}

record ApiRequestAuthentication(String appKey, long appId, long merchantId, String requestPath, String scope,
        boolean allowed, String reason, ApiQuotaUsage quotaUsage) {
}

@TableName("openapi_webhook_subscription")
record WebhookSubscription(@TableId(value = "subscription_id", type = IdType.INPUT) long subscriptionId, long appId,
        String eventType, String targetUrl, boolean active, Instant createdAt, Instant updatedAt) {
}

@TableName("openapi_webhook_delivery")
record WebhookDelivery(@TableId(value = "delivery_id", type = IdType.INPUT) long deliveryId, long subscriptionId,
        String eventId, WebhookDeliveryStatus status, int retryCount, Instant createdAt, Instant updatedAt) {
    WebhookDelivery mark(WebhookDeliveryStatus nextStatus) {
        return new WebhookDelivery(deliveryId, subscriptionId, eventId, nextStatus, retryCount + 1, createdAt,
                Instant.now());
    }
}
