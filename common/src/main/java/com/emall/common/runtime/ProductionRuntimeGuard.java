package com.emall.common.runtime;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public class ProductionRuntimeGuard implements ApplicationRunner {
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Set<String> UNSAFE_EXACT_VALUES =
            Set.of("root", "admin", "password", "secret", "changeme", "replace-me", "replace-in-production", "unknown");
    private static final Set<String> SHARDED_SERVICES = Set.of("user", "product", "inventory", "order", "cart",
            "payment", "pricing", "marketing", "search", "flash-sale");
    private static final List<String> DEFAULT_REQUIRED_PROPERTIES =
            List.of("emall.internal.operations-token", "emall.security.auth.token-secret");

    private final Environment environment;
    private final List<String> requiredProperties;

    public ProductionRuntimeGuard(Environment environment, List<String> requiredProperties) {
        this.environment = environment;
        this.requiredProperties = requiredProperties.isEmpty() ? DEFAULT_REQUIRED_PROPERTIES : requiredProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!productionGuardEnabled()) {
            return;
        }
        List<String> unsafe = new ArrayList<>();
        requiredProperties.forEach(property -> requireSecret(property, unsafe));
        requireTrue("emall.security.auth.enabled", unsafe);
        requireTrue("emall.security.auth.fail-closed-on-revocation-store-error", unsafe);
        requireTrue("emall.id.lease-enabled", unsafe);
        requireValue("spring.data.redis.cluster.nodes", unsafe);
        validateDataSource(unsafe);
        validateOptionalSecret("emall.security.field-encryption.key", unsafe);
        validateServiceSpecificConfiguration(unsafe);
        if (!unsafe.isEmpty()) {
            throw new IllegalStateException("production runtime guard failed, missing or unsafe properties: " + unsafe);
        }
    }

    private void validateDataSource(List<String> unsafe) {
        if (StringUtils.hasText(environment.getProperty("spring.datasource.url"))) {
            requireValue("spring.datasource.url", unsafe);
            requireDatabaseUsername("spring.datasource.username", unsafe);
            requirePassword("spring.datasource.password", unsafe);
        }
        if (environment.getProperty("emall.sharding.datasource.enabled", Boolean.class, false)) {
            requireValue("emall.sharding.datasource.jdbc-url-template", unsafe);
            requireDatabaseUsername("emall.sharding.datasource.username", unsafe);
            requirePassword("emall.sharding.datasource.password", unsafe);
        }
    }

    private void validateServiceSpecificConfiguration(List<String> unsafe) {
        String service = environment.getProperty("spring.application.name", "");
        if ("dubbo".equalsIgnoreCase(environment.getProperty("emall.rpc.protocol", "http"))) {
            requireExact("dubbo.protocol.port", "20880", unsafe);
        }
        if (SHARDED_SERVICES.contains(service)) {
            requireTrue("emall.sharding.enabled", unsafe);
            requireTrue("emall.sharding.datasource.enabled", unsafe);
        }
        if ("order".equals(service)) {
            requirePositiveInt("emall.capacity.order.max-submissions-per-user-per-minute", unsafe);
            requireTrue("emall.capacity.order.fail-closed", unsafe);
            requireTrue("emall.trust.identity.enabled", unsafe);
            requireTrue("emall.trust.identity.fail-closed", unsafe);
            requireTrue("emall.trust.risk.enabled", unsafe);
            requireTrue("emall.trust.risk.fail-closed", unsafe);
        }
        if ("payment".equals(service)) {
            requireExact("emall.payment.channel.mode", "http", unsafe);
            requireHttps("emall.payment.channel.base-url", unsafe);
            requireSecret("emall.payment.channel.api-key", unsafe);
            requireSecret("emall.payment.security.callback-secrets.default", unsafe);
            requireTrue("emall.payment.security.callback-signature-enabled", unsafe);
            requireTrue("emall.payment.security.replay-store-fail-closed", unsafe);
        }
        if ("flash-sale".equals(service)) {
            requireSecret("emall.flash-sale.security.token-secret", unsafe);
            requireTrue("emall.trust.identity.enabled", unsafe);
            requireTrue("emall.trust.identity.fail-closed", unsafe);
            requireTrue("emall.trust.risk.enabled", unsafe);
            requireTrue("emall.trust.risk.fail-closed", unsafe);
        }
        if ("gateway".equals(service)) {
            requireValue("emall.gateway.rate-limit.trusted-proxy-cidrs", unsafe);
            requirePositiveInt("emall.gateway.rate-limit.global-replenish-rate", unsafe);
            requirePositiveInt("emall.gateway.rate-limit.global-burst-capacity", unsafe);
            requirePositiveInt("emall.gateway.rate-limit.subject-replenish-rate", unsafe);
            requirePositiveInt("emall.gateway.rate-limit.subject-burst-capacity", unsafe);
            requirePositiveInt("emall.gateway.rate-limit.hot-resource-replenish-rate", unsafe);
            requirePositiveInt("emall.gateway.rate-limit.hot-resource-burst-capacity", unsafe);
        }
    }

    private void validateOptionalSecret(String property, List<String> unsafe) {
        if (environment.containsProperty(property)) {
            requireSecret(property, unsafe);
        }
    }

    private void requireValue(String property, List<String> unsafe) {
        if (isUnsafe(environment.getProperty(property))) {
            unsafe.add(property);
        }
    }

    private void requireDatabaseUsername(String property, List<String> unsafe) {
        String value = environment.getProperty(property);
        if (isUnsafe(value) || "root".equalsIgnoreCase(value) || "admin".equalsIgnoreCase(value)) {
            unsafe.add(property);
        }
    }

    private void requirePassword(String property, List<String> unsafe) {
        String value = environment.getProperty(property);
        if (isUnsafe(value) || value.getBytes(StandardCharsets.UTF_8).length < 16) {
            unsafe.add(property);
        }
    }

    private void requireSecret(String property, List<String> unsafe) {
        String value = environment.getProperty(property);
        if (isUnsafe(value) || value.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            unsafe.add(property);
        }
    }

    private void requireTrue(String property, List<String> unsafe) {
        if (!environment.getProperty(property, Boolean.class, false)) {
            unsafe.add(property);
        }
    }

    private void requireExact(String property, String expected, List<String> unsafe) {
        if (!expected.equalsIgnoreCase(environment.getProperty(property, ""))) {
            unsafe.add(property);
        }
    }

    private void requirePositiveInt(String property, List<String> unsafe) {
        if (environment.getProperty(property, Integer.class, 0) <= 0) {
            unsafe.add(property);
        }
    }

    private void requireHttps(String property, List<String> unsafe) {
        String value = environment.getProperty(property, "");
        if (!value.toLowerCase(Locale.ROOT).startsWith("https://") || isUnsafe(value)) {
            unsafe.add(property);
        }
    }

    private boolean isUnsafe(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return UNSAFE_EXACT_VALUES.contains(normalized) || normalized.startsWith("local-dev-")
                || normalized.startsWith("local-development-") || normalized.contains("replace-in-production")
                || normalized.contains("example-secret") || normalized.contains("example.com")
                || normalized.contains("localhost");
    }

    private boolean productionGuardEnabled() {
        if (environment.getProperty("emall.runtime.guard.enabled", Boolean.class, false)) {
            return true;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return "production".equalsIgnoreCase(environment.getProperty("emall.runtime.mode"));
    }
}
