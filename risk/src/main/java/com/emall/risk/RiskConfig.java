package com.emall.risk;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RiskConfig {
    @Bean
    SnowflakeIdGenerator riskIdGenerator() {
        return new SnowflakeIdGenerator(152L);
    }
}
