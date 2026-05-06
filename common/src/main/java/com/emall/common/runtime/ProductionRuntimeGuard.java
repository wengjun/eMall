package com.emall.common.runtime;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public class ProductionRuntimeGuard implements ApplicationRunner {
    private static final List<String> DEFAULT_REQUIRED_PROPERTIES = List.of(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "emall.internal.operations-token"
    );

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
        List<String> missing = new ArrayList<>();
        for (String property : requiredProperties) {
            String value = environment.getProperty(property);
            if (!StringUtils.hasText(value) || value.startsWith("local-dev-")) {
                missing.add(property);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("production runtime guard failed, missing unsafe properties: " + missing);
        }
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
