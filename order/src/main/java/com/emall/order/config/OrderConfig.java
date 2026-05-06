package com.emall.order.config;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.web.TraceIdClientHttpRequestInterceptor;
import com.emall.governance.recovery.AdaptiveRecoveryController;
import com.emall.governance.recovery.AdaptiveRecoveryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OrderConfig {
    @Bean
    SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(1L);
    }

    @Bean
    RestClient inventoryRestClient(@Value("${emall.downstream.inventory-url}") String inventoryUrl) {
        return RestClient.builder()
                .baseUrl(inventoryUrl)
                .requestInterceptor(new TraceIdClientHttpRequestInterceptor())
                .build();
    }

    @Bean
    RestClient pricingRestClient(@Value("${emall.downstream.pricing-url}") String pricingUrl) {
        return RestClient.builder()
                .baseUrl(pricingUrl)
                .requestInterceptor(new TraceIdClientHttpRequestInterceptor())
                .build();
    }

    @Bean
    RestClient marketingRestClient(@Value("${emall.downstream.marketing-url}") String marketingUrl) {
        return RestClient.builder()
                .baseUrl(marketingUrl)
                .requestInterceptor(new TraceIdClientHttpRequestInterceptor())
                .build();
    }

    @Bean
    AdaptiveRecoveryController inventoryRecoveryController() {
        return new AdaptiveRecoveryController(AdaptiveRecoveryPolicy.standard());
    }
}
