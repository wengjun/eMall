package com.emall.openapi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusOpenApiRepository implements OpenApiRepository {
    private final OpenApiMapper openApiMapper;
    private final OpenApiAppMapper appMapper;
    private final ApiQuotaUsageMapper quotaUsageMapper;
    private final WebhookSubscriptionMapper subscriptionMapper;

    MybatisPlusOpenApiRepository(OpenApiMapper openApiMapper, OpenApiAppMapper appMapper,
            ApiQuotaUsageMapper quotaUsageMapper, WebhookSubscriptionMapper subscriptionMapper) {
        this.openApiMapper = openApiMapper;
        this.appMapper = appMapper;
        this.quotaUsageMapper = quotaUsageMapper;
        this.subscriptionMapper = subscriptionMapper;
    }

    @Override
    public OpenApiApp saveApp(OpenApiApp app) {
        openApiMapper.saveApp(app);
        return app;
    }

    @Override
    public Optional<OpenApiApp> findApp(String appKey) {
        return Optional.ofNullable(appMapper.selectOne(new QueryWrapper<OpenApiApp>().eq("app_key", appKey)));
    }

    @Override
    public Optional<OpenApiApp> findApp(long appId) {
        return Optional.ofNullable(appMapper.selectById(appId));
    }

    @Override
    public ApiQuotaUsage saveQuota(ApiQuotaUsage usage) {
        openApiMapper.saveQuota(usage);
        return usage;
    }

    @Override
    public Optional<ApiQuotaUsage> findQuota(String appKey, LocalDate usageDate) {
        return Optional.ofNullable(quotaUsageMapper.selectOne(new QueryWrapper<ApiQuotaUsage>()
                .eq("app_key", appKey)
                .eq("usage_date", usageDate)));
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
        return Optional.ofNullable(subscriptionMapper.selectById(subscriptionId));
    }

    @Override
    public List<WebhookSubscription> findActiveSubscriptions(long appId) {
        return subscriptionMapper.selectList(new QueryWrapper<WebhookSubscription>()
                .eq("app_id", appId)
                .eq("active", true));
    }

    @Override
    public WebhookDelivery saveDelivery(WebhookDelivery delivery) {
        openApiMapper.saveDelivery(delivery);
        return delivery;
    }

}
