package com.emall.review.config;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReviewConfig {
    @Bean
    SnowflakeIdGenerator reviewIdGenerator() {
        return new SnowflakeIdGenerator(101L);
    }
}
