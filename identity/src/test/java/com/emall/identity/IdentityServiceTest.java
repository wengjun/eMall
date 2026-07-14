package com.emall.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.security.AuthSecurityProperties;
import com.emall.common.security.AuthTokenCodec;
import com.emall.common.security.AuthorizationGuard;
import com.emall.common.security.TokenRevocationStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class IdentityServiceTest {
    private final InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
    private final IdentityService service = identityService(repository);
    private static final String PASSWORD = "StrongPassword123";

    @Test
    void createsAccountSessionAndPermissionDecision() {
        IdentityAccount account =
                service.createAccount(IdentityType.PLATFORM_OPERATOR, "OpsUser", "Ops User", PASSWORD);
        AuthToken token = service.login("opsuser", PASSWORD, "device-1");
        service.grantPermission(account.accountId(), "order:read", "*");

        AccessDecision decision = service.checkAccess(account.accountId(), "order:read", "order:1001");

        assertThat(token.accessToken()).isNotBlank();
        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void revokesDeviceSession() {
        service.createAccount(IdentityType.CUSTOMER, "customer-1", "Customer One", PASSWORD);
        AuthToken token = service.login("customer-1", PASSWORD, "device-1");

        DeviceSession session = service.revokeSession(token.sessionId());

        assertThat(session.status()).isEqualTo(SessionStatus.REVOKED);
    }

    @Test
    void createsMerchantSubAccountAndServiceClient() {
        IdentityAccount account =
                service.createAccount(IdentityType.MERCHANT_OPERATOR, "seller-1", "Seller One", PASSWORD);

        MerchantSubAccount subAccount = service.createMerchantSubAccount(1001L, account.accountId(), "store-admin");
        ServiceClient client = service.registerServiceClient("merchant-app", "ServiceSecret12345", "order:read");

        assertThat(subAccount.roleCode()).isEqualTo("store-admin");
        assertThat(client.secretHash()).startsWith("$2").doesNotContain("ServiceSecret12345");
    }

    @Test
    void validatesActiveSessionWithPermissionGrant() {
        IdentityAccount account = service.createAccount(IdentityType.CUSTOMER, "customer-2", "Customer Two", PASSWORD);
        service.grantPermission(account.accountId(), "order:create", "user:" + account.accountId());
        AuthToken token = service.login("customer-2", PASSWORD, "device-2");

        SessionValidation validation =
                service.validateSession(token.accessToken(), "order:create", "user:" + account.accountId());

        assertThat(validation.allowed()).isTrue();
        assertThat(validation.accountId()).isEqualTo(account.accountId());
        assertThat(validation.deviceId()).isEqualTo("device-2");
    }

    @Test
    void rejectsWrongPasswordAndLocksAfterRepeatedFailures() {
        service.createAccount(IdentityType.CUSTOMER, "locked-user", "Locked User", PASSWORD);

        for (int attempt = 0; attempt < CredentialAttemptRecorder.MAXIMUM_ATTEMPTS; attempt++) {
            assertThatThrownBy(() -> service.login("locked-user", "WrongPassword123", "device-3"))
                    .hasMessageContaining("invalid");
        }

        assertThatThrownBy(() -> service.login("locked-user", PASSWORD, "device-3"))
                .hasMessageContaining("temporarily locked");
    }

    @Test
    void rotatesRefreshTokensAndRejectsReplay() {
        service.createAccount(IdentityType.CUSTOMER, "refresh-user", "Refresh User", PASSWORD);
        AuthToken original = service.login("refresh-user", PASSWORD, "device-4");

        AuthToken rotated = service.refresh(original.refreshToken(), "device-4");

        assertThat(rotated.refreshToken()).isNotEqualTo(original.refreshToken());
        assertThatThrownBy(() -> service.refresh(original.refreshToken(), "device-4"))
                .hasMessageContaining("expired, revoked, or device-bound");
    }

    @Test
    void allowsOnlyOneConcurrentRefreshTokenRotation() throws Exception {
        service.createAccount(IdentityType.CUSTOMER, "concurrent-refresh", "Concurrent Refresh", PASSWORD);
        AuthToken original = service.login("concurrent-refresh", PASSWORD, "device-5");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> refreshAfter(start, original));
            Future<Boolean> second = executor.submit(() -> refreshAfter(start, original));
            start.countDown();

            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean refreshAfter(CountDownLatch start, AuthToken token) throws InterruptedException {
        start.await();
        try {
            service.refresh(token.refreshToken(), "device-5");
            return true;
        } catch (com.emall.common.exception.BusinessException ex) {
            return false;
        }
    }

    private IdentityService identityService(IdentityRepository identityRepository) {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.setTokenSecret("identity-unit-test-token-secret-32-bytes");
        TokenRevocationStore revocationStore = new TokenRevocationStore() {
            @Override
            public boolean isRevoked(long sessionId) {
                return false;
            }

            @Override
            public void revoke(long sessionId, Duration ttl) {
            }
        };
        return new IdentityService(identityRepository, new SnowflakeIdGenerator(21L), new BCryptPasswordEncoder(),
                new AuthTokenCodec(new ObjectMapper(), properties), properties, revocationStore,
                new CredentialAttemptRecorder(identityRepository), AuthorizationGuard.noop());
    }
}
