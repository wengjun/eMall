package com.emall.order.config;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.web.OutboundHttpClientFactory;
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
    RestClient inventoryRestClient(OutboundHttpClientFactory httpClientFactory,
            @Value("${emall.downstream.inventory-url}") String inventoryUrl) {
        return httpClientFactory.restClient("inventory", inventoryUrl);
    }

    @Bean
    RestClient pricingRestClient(OutboundHttpClientFactory httpClientFactory,
            @Value("${emall.downstream.pricing-url}") String pricingUrl) {
        return httpClientFactory.restClient("pricing", pricingUrl);
    }

    @Bean
    RestClient marketingRestClient(OutboundHttpClientFactory httpClientFactory,
            @Value("${emall.downstream.marketing-url}") String marketingUrl) {
        return httpClientFactory.restClient("marketing", marketingUrl);
    }

    @Bean
    AdaptiveRecoveryController inventoryRecoveryController() {
        return new AdaptiveRecoveryController(AdaptiveRecoveryPolicy.standard());
    }
}
