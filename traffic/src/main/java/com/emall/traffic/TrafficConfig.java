package com.emall.traffic;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TrafficConfig {
    @Bean
    SnowflakeIdGenerator trafficIdGenerator() {
        return new SnowflakeIdGenerator(201L);
    }
}
