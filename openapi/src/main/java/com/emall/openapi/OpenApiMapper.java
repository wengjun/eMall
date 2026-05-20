package com.emall.openapi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface OpenApiMapper {
    @Insert("""
            INSERT INTO openapi_app
                (app_id, merchant_id, app_key, secret_hash, secret_ciphertext, name, scopes, daily_quota, active,
                created_at, updated_at)
            VALUES (#{app.appId}, #{app.merchantId}, #{app.appKey}, #{app.secretHash},
                #{app.secretCiphertext}, #{app.name}, #{app.scopes}, #{app.dailyQuota}, #{app.active},
                #{app.createdAt}, #{app.updatedAt})
            ON DUPLICATE KEY UPDATE secret_hash = VALUES(secret_hash),
                secret_ciphertext = VALUES(secret_ciphertext), name = VALUES(name), scopes = VALUES(scopes),
                daily_quota = VALUES(daily_quota), active = VALUES(active), updated_at = VALUES(updated_at)
            """)
    int saveApp(@Param("app") OpenApiApp app);

    @Select("""
            SELECT app_id, merchant_id, app_key, secret_hash, secret_ciphertext, name, scopes, daily_quota, active,
                created_at, updated_at
            FROM openapi_app
            WHERE app_key = #{appKey}
            """)
    OpenApiApp findAppByKey(@Param("appKey") String appKey);

    @Select("""
            SELECT app_id, merchant_id, app_key, secret_hash, secret_ciphertext, name, scopes, daily_quota, active,
                created_at, updated_at
            FROM openapi_app
            WHERE app_id = #{appId}
            """)
    OpenApiApp findAppById(@Param("appId") long appId);

    @Insert("""
            INSERT INTO openapi_quota_usage (app_key, usage_date, used_count, daily_quota, allowed)
            VALUES (#{usage.appKey}, #{usage.usageDate}, #{usage.usedCount}, #{usage.dailyQuota},
                #{usage.allowed})
            ON DUPLICATE KEY UPDATE used_count = VALUES(used_count), daily_quota = VALUES(daily_quota),
                allowed = VALUES(allowed)
            """)
    int saveQuota(@Param("usage") ApiQuotaUsage usage);

    @Select("""
            SELECT app_key, usage_date, used_count, daily_quota, allowed
            FROM openapi_quota_usage
            WHERE app_key = #{appKey} AND usage_date = #{usageDate}
            """)
    ApiQuotaUsage findQuota(@Param("appKey") String appKey, @Param("usageDate") LocalDate usageDate);

    @Insert("""
            INSERT IGNORE INTO openapi_nonce (app_key, nonce, request_path, expires_at, used_at)
            VALUES (#{appKey}, #{nonce}, #{requestPath}, #{expiresAt}, CURRENT_TIMESTAMP(6))
            """)
    int saveNonceIfAbsent(@Param("appKey") String appKey, @Param("nonce") String nonce,
            @Param("requestPath") String requestPath, @Param("expiresAt") Instant expiresAt);

    @Insert("""
            INSERT INTO openapi_webhook_subscription
                (subscription_id, app_id, event_type, target_url, active, created_at, updated_at)
            VALUES (#{subscription.subscriptionId}, #{subscription.appId}, #{subscription.eventType},
                #{subscription.targetUrl}, #{subscription.active}, #{subscription.createdAt},
                #{subscription.updatedAt})
            ON DUPLICATE KEY UPDATE target_url = VALUES(target_url), active = VALUES(active),
                updated_at = VALUES(updated_at)
            """)
    int saveSubscription(@Param("subscription") WebhookSubscription subscription);

    @Select("""
            SELECT subscription_id, app_id, event_type, target_url, active, created_at, updated_at
            FROM openapi_webhook_subscription
            WHERE subscription_id = #{subscriptionId}
            """)
    WebhookSubscription findSubscription(@Param("subscriptionId") long subscriptionId);

    @Select("""
            SELECT subscription_id, app_id, event_type, target_url, active, created_at, updated_at
            FROM openapi_webhook_subscription
            WHERE app_id = #{appId} AND active = TRUE
            """)
    List<WebhookSubscription> findActiveSubscriptions(@Param("appId") long appId);

    @Insert("""
            INSERT INTO openapi_webhook_delivery
                (delivery_id, subscription_id, event_id, status, retry_count, created_at, updated_at)
            VALUES (#{delivery.deliveryId}, #{delivery.subscriptionId}, #{delivery.eventId}, #{delivery.status},
                #{delivery.retryCount}, #{delivery.createdAt}, #{delivery.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), retry_count = VALUES(retry_count),
                updated_at = VALUES(updated_at)
            """)
    int saveDelivery(@Param("delivery") WebhookDelivery delivery);
}
