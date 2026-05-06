package com.emall.customerservice;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CustomerServiceConfig {
    @Bean
    SnowflakeIdGenerator customerServiceIdGenerator() {
        return new SnowflakeIdGenerator(183L);
    }
}
