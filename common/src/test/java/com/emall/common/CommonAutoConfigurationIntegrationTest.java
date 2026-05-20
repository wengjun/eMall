package com.emall.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.emall.common.crypto.FieldEncryptionAutoConfiguration;
import com.emall.common.crypto.FieldEncryptor;
import com.emall.common.idempotency.IdempotencyAutoConfiguration;
import com.emall.common.idempotency.IdempotencyRepository;
import com.emall.common.idempotency.IdempotencyService;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.metrics.BusinessMetricsAutoConfiguration;
import com.emall.common.mybatis.MybatisPlusAutoConfiguration;
import com.emall.common.region.OwnershipAutoConfiguration;
import com.emall.common.region.OwnershipGuard;
import com.emall.common.runtime.ProductionRuntimeGuard;
import com.emall.common.runtime.ProductionRuntimeGuardAutoConfiguration;
import com.emall.common.sharding.ShardRoutingAutoConfiguration;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.common.trust.IdentityAccessGuard;
import com.emall.common.trust.IdentityVerifier;
import com.emall.common.trust.RiskEvaluator;
import com.emall.common.trust.RiskGuard;
import com.emall.common.trust.TrustAutoConfiguration;
import com.emall.common.web.ApiSecurityHeadersFilter;
import com.emall.common.web.CommonWebAutoConfiguration;
import com.emall.common.web.CorrelationIdServletFilter;
import com.emall.common.web.IdempotencyKeyServletFilter;
import com.emall.common.web.OutboundClientAutoConfiguration;
import com.emall.common.web.OutboundHttpClientFactory;
import com.emall.common.web.RequestLoggingContextFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class CommonAutoConfigurationIntegrationTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                    .withConfiguration(AutoConfigurations.of(IdempotencyAutoConfiguration.class,
                            FieldEncryptionAutoConfiguration.class, OutboundClientAutoConfiguration.class,
                            TrustAutoConfiguration.class, BusinessMetricsAutoConfiguration.class,
                            OwnershipAutoConfiguration.class, ShardRoutingAutoConfiguration.class,
                            MybatisPlusAutoConfiguration.class, ProductionRuntimeGuardAutoConfiguration.class));

    @Test
    void shouldAutoConfigureCommonRuntimeInfrastructure() {
        contextRunner.withPropertyValues("emall.idempotency.processing-ttl=45s", "emall.idempotency.record-ttl=2d",
                "emall.http-client.connect-timeout=250ms", "emall.http-client.read-timeout=750ms",
                "emall.sharding.enabled=true", "emall.sharding.database-shard-count=4",
                "emall.sharding.logical-shard-count=16",
                "emall.runtime.guard.required-properties[0]=emall.internal.operations-token").run(context -> {
                    assertThat(context).hasSingleBean(IdempotencyRepository.class);
                    assertThat(context).hasSingleBean(IdempotencyService.class);
                    assertThat(context).hasSingleBean(Clock.class);
                    assertThat(context).hasSingleBean(FieldEncryptor.class);
                    assertThat(context).hasSingleBean(OutboundHttpClientFactory.class);
                    assertThat(context).hasSingleBean(IdentityVerifier.class);
                    assertThat(context).hasSingleBean(IdentityAccessGuard.class);
                    assertThat(context).hasSingleBean(RiskEvaluator.class);
                    assertThat(context).hasSingleBean(RiskGuard.class);
                    assertThat(context).hasSingleBean(BusinessMetrics.class);
                    assertThat(context).hasSingleBean(OwnershipGuard.class);
                    assertThat(context).hasSingleBean(ShardRoutingOperations.class);
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context).hasSingleBean(ProductionRuntimeGuard.class);
                    assertThat(context.getBean(OutboundHttpClientFactory.class).timeoutBudget())
                            .isEqualTo(Duration.ofSeconds(1));
                });
    }

    @Test
    void shouldAutoConfigureCommonServletFiltersInWebApplications() {
        new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(CommonWebAutoConfiguration.class))
                .run(context -> {
                    Set<Class<?>> filterTypes = context.getBeansOfType(FilterRegistrationBean.class).values().stream()
                            .map(registration -> registration.getFilter().getClass()).collect(Collectors.toSet());

                    assertThat(filterTypes).contains(CorrelationIdServletFilter.class, ApiSecurityHeadersFilter.class,
                            RequestLoggingContextFilter.class, IdempotencyKeyServletFilter.class);
                });
    }

    @Test
    void shouldPublishCommonAutoConfigurationsForServiceModules() throws IOException {
        String imports = Files.readString(Path.of(
                "src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));

        assertThat(imports).contains("com.emall.common.archive.ArchiveAutoConfiguration")
                .contains("com.emall.common.crypto.FieldEncryptionAutoConfiguration")
                .contains("com.emall.common.idempotency.IdempotencyAutoConfiguration")
                .contains("com.emall.common.metrics.BusinessMetricsAutoConfiguration")
                .contains("com.emall.common.mybatis.MybatisPlusAutoConfiguration")
                .contains("com.emall.common.region.OwnershipAutoConfiguration")
                .contains("com.emall.common.runtime.ProductionRuntimeGuardAutoConfiguration")
                .contains("com.emall.common.sharding.ShardRoutingAutoConfiguration")
                .contains("com.emall.common.trust.TrustAutoConfiguration")
                .contains("com.emall.common.web.OutboundClientAutoConfiguration")
                .contains("com.emall.common.web.CommonWebAutoConfiguration");
    }
}
