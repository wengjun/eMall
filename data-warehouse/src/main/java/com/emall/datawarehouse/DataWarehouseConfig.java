package com.emall.datawarehouse;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DataWarehouseConfig {
    @Bean
    SnowflakeIdGenerator dataWarehouseIdGenerator() {
        return new SnowflakeIdGenerator(192L);
    }
}
