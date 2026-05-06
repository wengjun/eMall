package com.emall.traffic;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TrafficMetrics {
    @Bean
    MeterBinder trafficMeterBinder(TrafficService trafficService) {
        return registry -> {
            Gauge.builder("emall_traffic_isolated_units", trafficService,
                    service -> service.summary().isolatedUnits()).register(registry);
            Gauge.builder("emall_traffic_running_shifts", trafficService,
                    service -> service.summary().runningShifts()).register(registry);
            Gauge.builder("emall_traffic_shard_routes", trafficService,
                    service -> service.summary().shardRoutes()).register(registry);
        };
    }
}
