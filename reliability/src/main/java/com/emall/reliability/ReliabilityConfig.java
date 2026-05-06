package com.emall.reliability;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ReliabilityConfig {
    @Bean
    SnowflakeIdGenerator reliabilityIdGenerator() {
        return new SnowflakeIdGenerator(202L);
    }
}
