package com.emall.common.security;

import java.time.Instant;
import java.util.Set;

public record AuthenticatedPrincipal(long accountId, long sessionId, String subject, String identityType,
        Set<String> scopes, Instant issuedAt, Instant expiresAt) {
    public AuthenticatedPrincipal {
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
    }

    public boolean hasScope(String requiredScope) {
        return scopes.contains("*") || scopes.contains(requiredScope);
    }

    public boolean isOperator() {
        return isPlatformOperator() || isMerchantOperator();
    }

    public boolean isPlatformOperator() {
        return "PLATFORM_OPERATOR".equals(identityType);
    }

    public boolean isMerchantOperator() {
        return "MERCHANT_OPERATOR".equals(identityType);
    }

    public boolean isServiceClient() {
        return "SERVICE_CLIENT".equals(identityType);
    }
}
