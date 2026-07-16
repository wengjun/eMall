package com.emall.common.runtime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ProductionRuntimeGuardTest {
    @Test
    void rejectsDefaultAccountsPlaceholdersAndWeakSecretsInProductionMode() {
        MockEnvironment environment = new MockEnvironment().withProperty("emall.runtime.mode", "production")
                .withProperty("spring.datasource.url", "jdbc:mysql://db/emall_order")
                .withProperty("spring.datasource.username", "root").withProperty("spring.datasource.password", "root")
                .withProperty("emall.internal.operations-token", "replace-in-production")
                .withProperty("emall.security.auth.token-secret", "local-dev-token");

        ProductionRuntimeGuard guard = new ProductionRuntimeGuard(environment, List.of());

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.username").hasMessageContaining("spring.datasource.password")
                .hasMessageContaining("emall.internal.operations-token")
                .hasMessageContaining("emall.security.auth.token-secret");
    }

    @Test
    void allowsCompleteStrongProductionConfiguration() {
        MockEnvironment environment = strongEnvironment();
        ProductionRuntimeGuard guard = new ProductionRuntimeGuard(environment, List.of());

        assertThatCode(() -> guard.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }

    @Test
    void rejectsPaymentTestAdapterAndInsecureChannelEndpointInProduction() {
        MockEnvironment environment = strongEnvironment().withProperty("spring.application.name", "payment")
                .withProperty("emall.payment.channel.mode", "memory")
                .withProperty("emall.payment.channel.base-url", "http://payment-channel")
                .withProperty("emall.payment.channel.api-key", "payment-channel-api-key-strong-value-123")
                .withProperty("emall.payment.security.callback-secrets.default",
                        "payment-callback-secret-strong-value-123")
                .withProperty("emall.payment.security.callback-signature-enabled", "true")
                .withProperty("emall.payment.security.replay-store-fail-closed", "true");

        ProductionRuntimeGuard guard = new ProductionRuntimeGuard(environment, List.of());

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .hasMessageContaining("emall.payment.channel.mode")
                .hasMessageContaining("emall.payment.channel.base-url");
    }

    @Test
    void rejectsRandomDubboPortInProduction() {
        MockEnvironment environment = strongEnvironment().withProperty("emall.rpc.protocol", "dubbo")
                .withProperty("dubbo.protocol.port", "-1");
        ProductionRuntimeGuard guard = new ProductionRuntimeGuard(environment, List.of());

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .hasMessageContaining("dubbo.protocol.port");
    }

    @Test
    void rejectsFlashSaleWhenIdentityOrRiskVerificationIsDisabled() {
        MockEnvironment environment = strongEnvironment().withProperty("spring.application.name", "flash-sale")
                .withProperty("emall.sharding.enabled", "true")
                .withProperty("emall.sharding.datasource.enabled", "true")
                .withProperty("emall.sharding.datasource.jdbc-url-template", "jdbc:mysql://db/{database}")
                .withProperty("emall.sharding.datasource.username", "emall_app")
                .withProperty("emall.sharding.datasource.password", "database-password-strong-123")
                .withProperty("emall.sharding.route-directory.endpoint", "http://routing:8117")
                .withProperty("emall.flash-sale.security.token-secret", "flash-sale-secret-strong-value-123456")
                .withProperty("emall.trust.identity.enabled", "false")
                .withProperty("emall.trust.identity.fail-closed", "true")
                .withProperty("emall.trust.risk.enabled", "true").withProperty("emall.trust.risk.fail-closed", "true");

        ProductionRuntimeGuard guard = new ProductionRuntimeGuard(environment, List.of());

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .hasMessageContaining("emall.trust.identity.enabled");
    }

    @Test
    void rejectsJdbcSearchInProduction() {
        MockEnvironment environment = strongEnvironment().withProperty("spring.application.name", "search")
                .withProperty("emall.sharding.enabled", "true")
                .withProperty("emall.sharding.datasource.enabled", "true")
                .withProperty("emall.sharding.datasource.jdbc-url-template", "jdbc:mysql://db/{database}")
                .withProperty("emall.sharding.datasource.username", "emall_app")
                .withProperty("emall.sharding.datasource.password", "database-password-strong-123")
                .withProperty("emall.sharding.route-directory.endpoint", "http://routing:8117")
                .withProperty("emall.search.engine", "jdbc");
        ProductionRuntimeGuard guard = new ProductionRuntimeGuard(environment, List.of());

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .hasMessageContaining("emall.search.engine");
    }

    @Test
    void rejectsShardedServiceWithoutPersistentRouteDirectory() {
        MockEnvironment environment = strongEnvironment().withProperty("spring.application.name", "product")
                .withProperty("emall.sharding.enabled", "true")
                .withProperty("emall.sharding.datasource.enabled", "true")
                .withProperty("emall.sharding.datasource.jdbc-url-template", "jdbc:mysql://db/{database}")
                .withProperty("emall.sharding.datasource.username", "emall_app")
                .withProperty("emall.sharding.datasource.password", "database-password-strong-123");
        ProductionRuntimeGuard guard = new ProductionRuntimeGuard(environment, List.of());

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .hasMessageContaining("emall.sharding.route-directory.endpoint");
    }

    @Test
    void validatesShardDurationPropertiesWithoutEnvironmentConverters() {
        MockEnvironment valid = strongEnvironment().withProperty("spring.application.name", "product")
                .withProperty("emall.sharding.enabled", "true")
                .withProperty("emall.sharding.datasource.enabled", "true")
                .withProperty("emall.sharding.datasource.jdbc-url-template", "jdbc:mysql://db/{database}")
                .withProperty("emall.sharding.datasource.username", "emall_app")
                .withProperty("emall.sharding.datasource.password", "database-password-strong-123")
                .withProperty("emall.sharding.route-directory.endpoint", "http://routing:8117")
                .withProperty("emall.sharding.mapping-cache-ttl", "PT30S")
                .withProperty("emall.sharding.minimum-cutover-delay", "PT1M");

        assertThatCode(() -> new ProductionRuntimeGuard(valid, List.of()).run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();

        MockEnvironment invalid = valid.withProperty("emall.sharding.mapping-cache-ttl", "invalid");
        assertThatThrownBy(() -> new ProductionRuntimeGuard(invalid, List.of()).run(new DefaultApplicationArguments()))
                .hasMessageContaining("emall.sharding.mapping-cache-ttl");
    }

    private MockEnvironment strongEnvironment() {
        return new MockEnvironment().withProperty("emall.runtime.guard.enabled", "true")
                .withProperty("emall.security.auth.enabled", "true")
                .withProperty("spring.data.redis.cluster.nodes", "redis-0:6379,redis-1:6379")
                .withProperty("spring.datasource.url", "jdbc:mysql://db/emall_order")
                .withProperty("spring.datasource.username", "emall_app")
                .withProperty("spring.datasource.password", "database-password-strong-123")
                .withProperty("emall.internal.operations-token", "operations-token-strong-value-123456")
                .withProperty("emall.security.auth.token-secret", "authentication-secret-strong-value-123")
                .withProperty("emall.security.auth.fail-closed-on-revocation-store-error", "true")
                .withProperty("emall.sharding.virtual-shard-count", "4096")
                .withProperty("emall.sharding.mapping-cache-ttl", "30s")
                .withProperty("emall.sharding.minimum-cutover-delay", "60s")
                .withProperty("emall.id.lease-enabled", "true");
    }
}
