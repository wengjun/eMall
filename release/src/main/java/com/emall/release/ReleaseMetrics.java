package com.emall.release;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ReleaseMetrics {
    @Bean
    MeterBinder releaseMeterBinder(ReleaseService releaseService) {
        return registry -> {
            Gauge.builder("emall_release_running_rollouts", releaseService,
                    service -> service.summary().runningRollouts()).register(registry);
            Gauge.builder("emall_release_enabled_flags", releaseService, service -> service.summary().enabledFlags())
                    .register(registry);
            Gauge.builder("emall_release_open_replays", releaseService, service -> service.summary().openReplays())
                    .register(registry);
            Gauge.builder("emall_release_guard_pauses", releaseService,
                    service -> service.guardSummary().pausedGuards()).register(registry);
            Gauge.builder("emall_release_guard_rollbacks", releaseService,
                    service -> service.guardSummary().rollbackGuards()).register(registry);
        };
    }
}
