package com.emall.openapi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryOpenApiRepository implements OpenApiRepository {
    private final ConcurrentMap<String, OpenApiApp> apps = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ApiQuotaUsage> quotas = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Instant> nonces = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, WebhookSubscription> subscriptions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, WebhookDelivery> deliveries = new ConcurrentHashMap<>();

    @Override
    public OpenApiApp saveApp(OpenApiApp app) {
        apps.put(app.appKey(), app);
        return app;
    }

    @Override
    public Optional<OpenApiApp> findApp(String appKey) {
        return Optional.ofNullable(apps.get(appKey));
    }

    @Override
    public Optional<OpenApiApp> findApp(long appId) {
        return apps.values().stream().filter(app -> app.appId() == appId).findFirst();
    }

    @Override
    public ApiQuotaUsage saveQuota(ApiQuotaUsage usage) {
        quotas.put(quotaKey(usage.appKey(), usage.usageDate()), usage);
        return usage;
    }

    @Override
    public Optional<ApiQuotaUsage> findQuota(String appKey, LocalDate usageDate) {
        return Optional.ofNullable(quotas.get(quotaKey(appKey, usageDate)));
    }

    @Override
    public boolean saveNonceIfAbsent(String appKey, String nonce, String requestPath, Instant expiresAt) {
        return nonces.putIfAbsent(appKey + ":" + nonce, expiresAt) == null;
    }

    @Override
    public WebhookSubscription saveSubscription(WebhookSubscription subscription) {
        subscriptions.put(subscription.subscriptionId(), subscription);
        return subscription;
    }

    @Override
    public Optional<WebhookSubscription> findSubscription(long subscriptionId) {
        return Optional.ofNullable(subscriptions.get(subscriptionId));
    }

    @Override
    public List<WebhookSubscription> findActiveSubscriptions(long appId) {
        return subscriptions.values().stream().filter(subscription -> subscription.appId() == appId)
                .filter(WebhookSubscription::active).toList();
    }

    @Override
    public WebhookDelivery saveDelivery(WebhookDelivery delivery) {
        deliveries.put(delivery.deliveryId(), delivery);
        return delivery;
    }

    private String quotaKey(String appKey, LocalDate usageDate) {
        return appKey + ":" + usageDate;
    }
}
