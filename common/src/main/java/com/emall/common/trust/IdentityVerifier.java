package com.emall.common.trust;

@FunctionalInterface
public interface IdentityVerifier {
    IdentityAccessDecision verify(IdentityAccessRequest request);

    static IdentityVerifier noop() {
        return request -> IdentityAccessDecision.allow(request.expectedAccountId(),
                String.valueOf(request.expectedAccountId()), request.deviceId());
    }
}
