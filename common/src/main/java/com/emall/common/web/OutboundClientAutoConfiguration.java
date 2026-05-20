package com.emall.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(OutboundHttpClientProperties.class)
public class OutboundClientAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public OutboundHttpClientFactory outboundHttpClientFactory(OutboundHttpClientProperties properties) {
        return new OutboundHttpClientFactory(properties);
    }
}
