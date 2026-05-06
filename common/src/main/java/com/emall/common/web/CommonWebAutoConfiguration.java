package com.emall.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({FilterRegistrationBean.class, OncePerRequestFilter.class})
public class CommonWebAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    FilterRegistrationBean<CorrelationIdServletFilter> correlationIdServletFilter() {
        FilterRegistrationBean<CorrelationIdServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationIdServletFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    FilterRegistrationBean<ApiSecurityHeadersFilter> apiSecurityHeadersFilter() {
        FilterRegistrationBean<ApiSecurityHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiSecurityHeadersFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    FilterRegistrationBean<RequestLoggingContextFilter> requestLoggingContextFilter() {
        FilterRegistrationBean<RequestLoggingContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestLoggingContextFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
