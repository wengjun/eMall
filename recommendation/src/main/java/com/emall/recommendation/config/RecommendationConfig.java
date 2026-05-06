package com.emall.recommendation.config;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecommendationConfig {
    @Bean
    SnowflakeIdGenerator recommendationIdGenerator() {
        return new SnowflakeIdGenerator(131L);
    }
}
