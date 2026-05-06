package com.emall.operations;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OperationsConfig {
    @Bean
    SnowflakeIdGenerator operationsIdGenerator() {
        return new SnowflakeIdGenerator(153L);
    }
}
