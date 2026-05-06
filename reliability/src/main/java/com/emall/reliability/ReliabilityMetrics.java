package com.emall.reliability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ReliabilityMetrics {
    @Bean
    MeterBinder reliabilityMeterBinder(ReliabilityService reliabilityService) {
        return registry -> {
            Gauge.builder("emall_reliability_blocked_readiness_gates", reliabilityService,
                    service -> service.summary().blockedReadinessGates()).register(registry);
            Gauge.builder("emall_reliability_approved_chaos", reliabilityService,
                    service -> service.summary().approvedChaos()).register(registry);
            Gauge.builder("emall_reliability_slo_objectives", reliabilityService,
                    service -> service.summary().sloObjectives()).register(registry);
        };
    }
}
