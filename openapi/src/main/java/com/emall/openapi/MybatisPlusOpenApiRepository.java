package com.emall.openapi;

import static com.emall.common.persistence.RowMaps.booleanValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.localDateValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
        return Optional.ofNullable(openApiMapper.findAppByKey(appKey)).map(this::mapApp);
    }

    @Override
    public Optional<OpenApiApp> findApp(long appId) {
        return Optional.ofNullable(openApiMapper.findAppById(appId)).map(this::mapApp);
    }

    @Override
    public ApiQuotaUsage saveQuota(ApiQuotaUsage usage) {
        openApiMapper.saveQuota(usage);
        return usage;
    }

    @Override
    public Optional<ApiQuotaUsage> findQuota(String appKey, LocalDate usageDate) {
        return Optional.ofNullable(openApiMapper.findQuota(appKey, usageDate)).map(this::mapQuota);
    }

    @Override
    public WebhookSubscription saveSubscription(WebhookSubscription subscription) {
        openApiMapper.saveSubscription(subscription);
        return subscription;
    }

    @Override
    public Optional<WebhookSubscription> findSubscription(long subscriptionId) {
        return Optional.ofNullable(openApiMapper.findSubscription(subscriptionId)).map(this::mapSubscription);
    }

    @Override
    public List<WebhookSubscription> findActiveSubscriptions(long appId) {
        return openApiMapper.findActiveSubscriptions(appId).stream().map(this::mapSubscription).toList();
    }

    @Override
    public WebhookDelivery saveDelivery(WebhookDelivery delivery) {
        openApiMapper.saveDelivery(delivery);
        return delivery;
    }

    private OpenApiApp mapApp(Map<String, Object> row) {
        return new OpenApiApp(longValue(row, "app_id"), longValue(row, "merchant_id"), stringValue(row, "app_key"),
                stringValue(row, "secret_hash"), stringValue(row, "name"), stringValue(row, "scopes"),
                intValue(row, "daily_quota"), booleanValue(row, "active"), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private ApiQuotaUsage mapQuota(Map<String, Object> row) {
        return new ApiQuotaUsage(stringValue(row, "app_key"), localDateValue(row, "usage_date"),
                intValue(row, "used_count"), intValue(row, "daily_quota"), booleanValue(row, "allowed"));
    }

    private WebhookSubscription mapSubscription(Map<String, Object> row) {
        return new WebhookSubscription(longValue(row, "subscription_id"), longValue(row, "app_id"),
                stringValue(row, "event_type"), stringValue(row, "target_url"), booleanValue(row, "active"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }
}
