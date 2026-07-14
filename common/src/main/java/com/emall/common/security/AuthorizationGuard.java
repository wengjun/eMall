package com.emall.common.security;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.util.Arrays;

public class AuthorizationGuard {
    private final AuthSecurityProperties properties;

    public AuthorizationGuard(AuthSecurityProperties properties) {
        this.properties = properties;
    }

    public static AuthorizationGuard noop() {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.setEnabled(false);
        return new AuthorizationGuard(properties);
    }

    public AuthenticatedPrincipal requireAuthenticated() {
        if (!properties.isEnabled()) {
            return null;
        }
        return AuthenticationContext.current()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "authentication is required"));
    }

    public void requireAccount(long accountId) {
        AuthenticatedPrincipal principal = requireAuthenticated();
        if (principal != null && principal.accountId() != accountId && !principal.isPlatformOperator()
                && !principal.isServiceClient()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "resource does not belong to the authenticated account");
        }
    }

    public void requireOwnerOrOperator(long accountId) {
        requireAccount(accountId);
    }

    public void requireOperator() {
        AuthenticatedPrincipal principal = requireAuthenticated();
        if (principal != null && !principal.isPlatformOperator()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "platform operator role is required");
        }
    }

    public void requireServiceOrOperator() {
        AuthenticatedPrincipal principal = requireAuthenticated();
        if (principal != null && !principal.isServiceClient() && !principal.isPlatformOperator()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "service or operator role is required");
        }
    }

    public void requireScope(String scope) {
        AuthenticatedPrincipal principal = requireAuthenticated();
        if (principal != null && !principal.hasScope(scope)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "required scope is missing: " + scope);
        }
    }

    public void requireIdentityType(String... allowedTypes) {
        AuthenticatedPrincipal principal = requireAuthenticated();
        if (principal != null && Arrays.stream(allowedTypes).noneMatch(principal.identityType()::equals)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "identity type is not allowed");
        }
    }

    public long accountIdOr(long fallbackAccountId) {
        return properties.isEnabled() ? requireAuthenticated().accountId() : fallbackAccountId;
    }
}
