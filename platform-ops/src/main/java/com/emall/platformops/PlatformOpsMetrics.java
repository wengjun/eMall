package com.emall.platformops;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PlatformOpsMetrics {
    @Bean
    MeterBinder platformOpsMeterBinder(PlatformOpsService platformOpsService) {
        return registry -> {
            Gauge.builder("emall_platform_ops_critical_security_signals", platformOpsService,
                    service -> service.summary().criticalSecuritySignals()).register(registry);
            Gauge.builder("emall_platform_ops_blocked_database_ops", platformOpsService,
                    service -> service.summary().blockedDatabaseOps()).register(registry);
            Gauge.builder("emall_platform_ops_approved_finops_actions", platformOpsService,
                    service -> service.summary().approvedFinOpsActions()).register(registry);
        };
    }
}
