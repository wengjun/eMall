package com.emall.common.runtime;

import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@EnableConfigurationProperties(ProductionRuntimeGuardProperties.class)
public class ProductionRuntimeGuardAutoConfiguration {
    @Bean
    public ProductionRuntimeGuard productionRuntimeGuard(Environment environment,
                                                         ProductionRuntimeGuardProperties properties) {
        return new ProductionRuntimeGuard(environment, properties.requiredProperties());
    }
}

@ConfigurationProperties(prefix = "emall.runtime.guard")
record ProductionRuntimeGuardProperties(List<String> requiredProperties) {
    ProductionRuntimeGuardProperties {
        requiredProperties = requiredProperties == null ? List.of() : List.copyOf(requiredProperties);
    }
}
