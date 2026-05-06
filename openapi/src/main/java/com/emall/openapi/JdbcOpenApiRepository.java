package com.emall.openapi;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcOpenApiRepository implements OpenApiRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcOpenApiRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OpenApiApp saveApp(OpenApiApp app) {
        jdbcTemplate.update("""
                INSERT INTO openapi_app
                    (app_id, merchant_id, app_key, secret_hash, name, scopes, daily_quota, active,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name), scopes = VALUES(scopes),
                    daily_quota = VALUES(daily_quota), active = VALUES(active), updated_at = VALUES(updated_at)
                """, app.appId(), app.merchantId(), app.appKey(), app.secretHash(), app.name(), app.scopes(),
                app.dailyQuota(), app.active(), Timestamp.from(app.createdAt()), Timestamp.from(app.updatedAt()));
        return app;
    }

    @Override
    public Optional<OpenApiApp> findApp(String appKey) {
        return jdbcTemplate.query("SELECT * FROM openapi_app WHERE app_key = ?", this::mapApp, appKey)
                .stream().findFirst();
    }

    @Override
    public Optional<OpenApiApp> findApp(long appId) {
        return jdbcTemplate.query("SELECT * FROM openapi_app WHERE app_id = ?", this::mapApp, appId)
                .stream().findFirst();
    }

    @Override
    public ApiQuotaUsage saveQuota(ApiQuotaUsage usage) {
        jdbcTemplate.update("""
                INSERT INTO openapi_quota_usage (app_key, usage_date, used_count, daily_quota, allowed)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE used_count = VALUES(used_count), daily_quota = VALUES(daily_quota),
                    allowed = VALUES(allowed)
                """, usage.appKey(), Date.valueOf(usage.usageDate()), usage.usedCount(), usage.dailyQuota(),
                usage.allowed());
        return usage;
    }

    @Override
    public Optional<ApiQuotaUsage> findQuota(String appKey, LocalDate usageDate) {
        return jdbcTemplate.query("""
                SELECT * FROM openapi_quota_usage
                WHERE app_key = ? AND usage_date = ?
                """, this::mapQuota, appKey, Date.valueOf(usageDate)).stream().findFirst();
    }

    @Override
    public WebhookSubscription saveSubscription(WebhookSubscription subscription) {
        jdbcTemplate.update("""
                INSERT INTO openapi_webhook_subscription
                    (subscription_id, app_id, event_type, target_url, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE target_url = VALUES(target_url), active = VALUES(active),
                    updated_at = VALUES(updated_at)
                """, subscription.subscriptionId(), subscription.appId(), subscription.eventType(),
                subscription.targetUrl(), subscription.active(), Timestamp.from(subscription.createdAt()),
                Timestamp.from(subscription.updatedAt()));
        return subscription;
    }

    @Override
    public Optional<WebhookSubscription> findSubscription(long subscriptionId) {
        return jdbcTemplate.query("SELECT * FROM openapi_webhook_subscription WHERE subscription_id = ?",
                this::mapSubscription, subscriptionId).stream().findFirst();
    }

    @Override
    public List<WebhookSubscription> findActiveSubscriptions(long appId) {
        return jdbcTemplate.query("""
                SELECT * FROM openapi_webhook_subscription
                WHERE app_id = ? AND active = TRUE
                """, this::mapSubscription, appId);
    }

    @Override
    public WebhookDelivery saveDelivery(WebhookDelivery delivery) {
        jdbcTemplate.update("""
                INSERT INTO openapi_webhook_delivery
                    (delivery_id, subscription_id, event_id, status, retry_count, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), retry_count = VALUES(retry_count),
                    updated_at = VALUES(updated_at)
                """, delivery.deliveryId(), delivery.subscriptionId(), delivery.eventId(), delivery.status().name(),
                delivery.retryCount(), Timestamp.from(delivery.createdAt()), Timestamp.from(delivery.updatedAt()));
        return delivery;
    }

    private OpenApiApp mapApp(ResultSet rs, int rowNum) throws SQLException {
        return new OpenApiApp(rs.getLong("app_id"), rs.getLong("merchant_id"), rs.getString("app_key"),
                rs.getString("secret_hash"), rs.getString("name"), rs.getString("scopes"),
                rs.getInt("daily_quota"), rs.getBoolean("active"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private ApiQuotaUsage mapQuota(ResultSet rs, int rowNum) throws SQLException {
        return new ApiQuotaUsage(rs.getString("app_key"), rs.getDate("usage_date").toLocalDate(),
                rs.getInt("used_count"), rs.getInt("daily_quota"), rs.getBoolean("allowed"));
    }

    private WebhookSubscription mapSubscription(ResultSet rs, int rowNum) throws SQLException {
        return new WebhookSubscription(rs.getLong("subscription_id"), rs.getLong("app_id"),
                rs.getString("event_type"), rs.getString("target_url"), rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
