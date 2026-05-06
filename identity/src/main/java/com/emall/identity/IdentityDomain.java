package com.emall.identity;

import java.time.Instant;

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

record IdentityAccount(
        long accountId,
        IdentityType type,
        String subject,
        String displayName,
        IdentityStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    IdentityAccount changeStatus(IdentityStatus nextStatus) {
        return new IdentityAccount(accountId, type, subject, displayName, nextStatus, createdAt, Instant.now());
    }
}

record DeviceSession(
        long sessionId,
        long accountId,
        String deviceId,
        String accessToken,
        String refreshToken,
        SessionStatus status,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
    DeviceSession revoke() {
        return new DeviceSession(sessionId, accountId, deviceId, accessToken, refreshToken, SessionStatus.REVOKED,
                expiresAt, createdAt, Instant.now());
    }
}

record PermissionGrant(
        long grantId,
        long accountId,
        String scope,
        String resource,
        Instant createdAt
) {
}

record ServiceClient(
        long clientId,
        String clientKey,
        String secretHash,
        String scopes,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

record MerchantSubAccount(
        long subAccountId,
        long merchantId,
        long accountId,
        String roleCode,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

record AuthToken(
        long sessionId,
        String accessToken,
        String refreshToken,
        Instant expiresAt
) {
}

record AccessDecision(
        long accountId,
        String scope,
        String resource,
        boolean allowed
) {
}
