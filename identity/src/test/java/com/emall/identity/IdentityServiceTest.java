package com.emall.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;

class IdentityServiceTest {
    private final InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
    private final IdentityService service = new IdentityService(repository, new SnowflakeIdGenerator(21L));

    @Test
    void createsAccountSessionAndPermissionDecision() {
        IdentityAccount account = service.createAccount(IdentityType.PLATFORM_OPERATOR, "OpsUser", "Ops User");
        AuthToken token = service.login("opsuser", "device-1");
        service.grantPermission(account.accountId(), "order:read", "*");

        AccessDecision decision = service.checkAccess(account.accountId(), "order:read", "order:1001");

        assertThat(token.accessToken()).isNotBlank();
        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void revokesDeviceSession() {
        service.createAccount(IdentityType.CUSTOMER, "customer-1", "Customer One");
        AuthToken token = service.login("customer-1", "device-1");

        DeviceSession session = service.revokeSession(token.sessionId());

        assertThat(session.status()).isEqualTo(SessionStatus.REVOKED);
    }

    @Test
    void createsMerchantSubAccountAndServiceClient() {
        IdentityAccount account = service.createAccount(IdentityType.MERCHANT_OPERATOR, "seller-1", "Seller One");

        MerchantSubAccount subAccount = service.createMerchantSubAccount(1001L, account.accountId(), "store-admin");
        ServiceClient client = service.registerServiceClient("merchant-app", "secret", "order:read");

        assertThat(subAccount.roleCode()).isEqualTo("store-admin");
        assertThat(client.secretHash()).hasSize(64);
    }
}
