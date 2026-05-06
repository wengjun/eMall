package com.emall.openapi;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {
    @Bean
    SnowflakeIdGenerator openApiIdGenerator() {
        return new SnowflakeIdGenerator(154L);
    }
}
