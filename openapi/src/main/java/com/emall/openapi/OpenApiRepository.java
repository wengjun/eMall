package com.emall.openapi;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface OpenApiRepository {
    OpenApiApp saveApp(OpenApiApp app);

    Optional<OpenApiApp> findApp(String appKey);

    Optional<OpenApiApp> findApp(long appId);

    ApiQuotaUsage saveQuota(ApiQuotaUsage usage);

    Optional<ApiQuotaUsage> findQuota(String appKey, LocalDate usageDate);

    WebhookSubscription saveSubscription(WebhookSubscription subscription);

    Optional<WebhookSubscription> findSubscription(long subscriptionId);

    List<WebhookSubscription> findActiveSubscriptions(long appId);

    WebhookDelivery saveDelivery(WebhookDelivery delivery);
}
