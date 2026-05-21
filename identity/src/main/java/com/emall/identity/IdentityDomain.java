package com.emall.identity;

import java.time.Instant;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

enum IdentityType {
    CUSTOMER,
    MERCHANT_OPERATOR,
    PLATFORM_OPERATOR,
    SERVICE_CLIENT
}

enum IdentityStatus {
    ACTIVE,
    LOCKED,
    CLOSED
}

enum SessionStatus {
    ACTIVE,
    REVOKED,
    EXPIRED
}

@TableName("identity_account")
record IdentityAccount(@TableId(value = "account_id", type = IdType.INPUT) long accountId,
        @TableField("identity_type") IdentityType type, String subject, String displayName, IdentityStatus status,
        Instant createdAt, Instant updatedAt) {
    IdentityAccount changeStatus(IdentityStatus nextStatus) {
        return new IdentityAccount(accountId, type, subject, displayName, nextStatus, createdAt, Instant.now());
    }
}

@TableName("identity_device_session")
record DeviceSession(@TableId(value = "session_id", type = IdType.INPUT) long sessionId, long accountId,
        String deviceId, String accessToken, String refreshToken, SessionStatus status, Instant expiresAt,
        Instant createdAt, Instant updatedAt) {
    DeviceSession revoke() {
        return new DeviceSession(sessionId, accountId, deviceId, accessToken, refreshToken, SessionStatus.REVOKED,
                expiresAt, createdAt, Instant.now());
    }
}

@TableName("identity_permission_grant")
record PermissionGrant(@TableId(value = "grant_id", type = IdType.INPUT) long grantId, long accountId, String scope,
        String resource, Instant createdAt) {
}

@TableName("identity_service_client")
record ServiceClient(@TableId(value = "client_id", type = IdType.INPUT) long clientId, String clientKey,
        String secretHash, String scopes, boolean active, Instant createdAt, Instant updatedAt) {
}

@TableName("identity_merchant_sub_account")
record MerchantSubAccount(@TableId(value = "sub_account_id", type = IdType.INPUT) long subAccountId, long merchantId,
        long accountId, String roleCode, boolean active, Instant createdAt, Instant updatedAt) {
}

record AuthToken(long sessionId, String accessToken, String refreshToken, Instant expiresAt) {
}

record AccessDecision(long accountId, String scope, String resource, boolean allowed) {
}

record SessionValidation(long accountId, String subject, String deviceId, boolean allowed, String reason) {
}
