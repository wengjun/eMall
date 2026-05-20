package com.emall.common.trust;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;

public class IdentityAccessGuard {
    private final IdentityVerifier identityVerifier;
    private final IdentityTrustProperties properties;

    public IdentityAccessGuard(IdentityVerifier identityVerifier, IdentityTrustProperties properties) {
        this.identityVerifier = identityVerifier;
        this.properties = properties;
    }

    public static IdentityAccessGuard noop() {
        return new IdentityAccessGuard(IdentityVerifier.noop(), new IdentityTrustProperties());
    }

    public IdentityAccessDecision requireAccess(ClientTrustContext context, long expectedAccountId, String scope,
            String resource) {
        if (!properties.isEnabled()) {
            return IdentityAccessDecision.allow(expectedAccountId, String.valueOf(expectedAccountId),
                    context == null ? ClientTrustContext.UNKNOWN_DEVICE : context.deviceId());
        }
        ClientTrustContext safeContext = context == null
                ? ClientTrustContext.anonymous().withDefaults(expectedAccountId, null, null)
                : context.withDefaults(expectedAccountId, context.deviceId(), context.channel());
        if (safeContext.accessToken() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "identity access token is required");
        }
        IdentityAccessDecision decision = identityVerifier.verify(new IdentityAccessRequest(safeContext.accessToken(),
                expectedAccountId, scope, resource, safeContext.deviceId(), safeContext.sourceIp()));
        if (decision.accountId() != expectedAccountId) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "identity account mismatch");
        }
        if (!decision.allowed()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "identity access denied: " + decision.reason());
        }
        return decision;
    }
}
