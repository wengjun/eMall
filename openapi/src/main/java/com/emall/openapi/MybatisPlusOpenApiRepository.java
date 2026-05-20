package com.emall.openapi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusOpenApiRepository implements OpenApiRepository {
    private final OpenApiMapper openApiMapper;

    MybatisPlusOpenApiRepository(OpenApiMapper openApiMapper) {
        this.openApiMapper = openApiMapper;
    }

    @Override
    public OpenApiApp saveApp(OpenApiApp app) {
        openApiMapper.saveApp(app);
        return app;
    }

    @Override
    public Optional<OpenApiApp> findApp(String appKey) {
        return Optional.ofNullable(openApiMapper.findAppByKey(appKey));
    }

    @Override
    public Optional<OpenApiApp> findApp(long appId) {
        return Optional.ofNullable(openApiMapper.findAppById(appId));
    }

    @Override
    public ApiQuotaUsage saveQuota(ApiQuotaUsage usage) {
        openApiMapper.saveQuota(usage);
        return usage;
    }

    @Override
    public Optional<ApiQuotaUsage> findQuota(String appKey, LocalDate usageDate) {
        return Optional.ofNullable(openApiMapper.findQuota(appKey, usageDate));
    }

    @Override
    public boolean saveNonceIfAbsent(String appKey, String nonce, String requestPath, Instant expiresAt) {
        return openApiMapper.saveNonceIfAbsent(appKey, nonce, requestPath, expiresAt) == 1;
    }

    @Override
    public WebhookSubscription saveSubscription(WebhookSubscription subscription) {
        openApiMapper.saveSubscription(subscription);
        return subscription;
    }

    @Override
    public Optional<WebhookSubscription> findSubscription(long subscriptionId) {
        return Optional.ofNullable(openApiMapper.findSubscription(subscriptionId));
    }

    @Override
    public List<WebhookSubscription> findActiveSubscriptions(long appId) {
        return openApiMapper.findActiveSubscriptions(appId);
    }

    @Override
    public WebhookDelivery saveDelivery(WebhookDelivery delivery) {
        openApiMapper.saveDelivery(delivery);
        return delivery;
    }

}
