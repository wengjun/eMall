package com.emall.finance;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FinanceConfig {
    @Bean
    SnowflakeIdGenerator financeIdGenerator() {
        return new SnowflakeIdGenerator(182L);
    }
}
