package com.emall.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@EnableConfigurationProperties(AuthSecurityProperties.class)
public class AuthSecurityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    AuthTokenCodec authTokenCodec(ObjectMapper objectMapper, AuthSecurityProperties properties) {
        return new AuthTokenCodec(objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    TokenRevocationStore tokenRevocationStore(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        return redisTemplate == null ? new NoopTokenRevocationStore() : new RedisTokenRevocationStore(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    AuthorizationGuard authorizationGuard(AuthSecurityProperties properties) {
        return new AuthorizationGuard(properties);
    }

    @Bean
    @ConditionalOnClass(FilterRegistrationBean.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(name = "apiAuthenticationFilter")
    FilterRegistrationBean<ApiAuthenticationFilter> apiAuthenticationFilter(AuthTokenCodec tokenCodec,
            TokenRevocationStore revocationStore, AuthSecurityProperties properties, ObjectMapper objectMapper) {
        FilterRegistrationBean<ApiAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiAuthenticationFilter(tokenCodec, revocationStore, properties, objectMapper));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
