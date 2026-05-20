package com.emall.common.trust;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class IdentityAccessGuardTest {
    @Test
    void deniesMissingTokenWhenIdentityIsEnabled() {
        IdentityTrustProperties properties = new IdentityTrustProperties();
        properties.setEnabled(true);
        IdentityAccessGuard guard = new IdentityAccessGuard(IdentityVerifier.noop(), properties);

        assertThatThrownBy(() -> guard.requireAccess(ClientTrustContext.of(1001L, null, "device-1", "127.0.0.1", "app"),
                1001L, "order:create", "user:1001")).isInstanceOf(BusinessException.class)
                .hasMessageContaining("access token");
    }

    @Test
    void rejectsAccountMismatchFromVerifier() {
        IdentityTrustProperties properties = new IdentityTrustProperties();
        properties.setEnabled(true);
        IdentityAccessGuard guard = new IdentityAccessGuard(
                request -> IdentityAccessDecision.allow(2002L, "user-2", request.deviceId()), properties);

        assertThatThrownBy(
                () -> guard.requireAccess(ClientTrustContext.of(1001L, "token-1", "device-1", "127.0.0.1", "app"),
                        1001L, "order:create", "user:1001"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("account mismatch");
    }

    @Test
    void allowsWhenVerifierAllowsExpectedAccount() {
        IdentityTrustProperties properties = new IdentityTrustProperties();
        properties.setEnabled(true);
        IdentityAccessGuard guard = new IdentityAccessGuard(
                request -> IdentityAccessDecision.allow(request.expectedAccountId(), "user-1", request.deviceId()),
                properties);

        IdentityAccessDecision decision =
                guard.requireAccess(ClientTrustContext.of(1001L, "token-1", "device-1", "127.0.0.1", "app"), 1001L,
                        "order:create", "user:1001");

        assertThat(decision.allowed()).isTrue();
    }
}
