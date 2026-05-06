package com.emall.forecasting;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ForecastingConfig {
    @Bean
    SnowflakeIdGenerator forecastingIdGenerator() {
        return new SnowflakeIdGenerator(184L);
    }
}
