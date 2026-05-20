package com.emall.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({FilterRegistrationBean.class, OncePerRequestFilter.class})
@EnableConfigurationProperties(IdempotencyHttpProperties.class)
public class CommonWebAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = "correlationIdServletFilter")
    FilterRegistrationBean<CorrelationIdServletFilter> correlationIdServletFilter() {
        FilterRegistrationBean<CorrelationIdServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationIdServletFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(name = "apiSecurityHeadersFilter")
    FilterRegistrationBean<ApiSecurityHeadersFilter> apiSecurityHeadersFilter() {
        FilterRegistrationBean<ApiSecurityHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiSecurityHeadersFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(name = "requestLoggingContextFilter")
    FilterRegistrationBean<RequestLoggingContextFilter> requestLoggingContextFilter() {
        FilterRegistrationBean<RequestLoggingContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestLoggingContextFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(name = "idempotencyKeyServletFilter")
    FilterRegistrationBean<IdempotencyKeyServletFilter> idempotencyKeyServletFilter(
            IdempotencyHttpProperties properties) {
        FilterRegistrationBean<IdempotencyKeyServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new IdempotencyKeyServletFilter(properties));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
