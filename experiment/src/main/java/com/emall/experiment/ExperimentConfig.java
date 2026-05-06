package com.emall.experiment;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ExperimentConfig {
    @Bean
    SnowflakeIdGenerator experimentIdGenerator() {
        return new SnowflakeIdGenerator(163L);
    }
}
