package com.emall.common.trust;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@EnableConfigurationProperties({IdentityTrustProperties.class, RiskTrustProperties.class})
public class TrustAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    IdentityVerifier identityVerifier(IdentityTrustProperties properties, ObjectProvider<RestClient.Builder> builder) {
        if (!properties.isEnabled()) {
            return IdentityVerifier.noop();
        }
        return new RemoteIdentityVerifier(
                builder.getIfAvailable(RestClient::builder).baseUrl(properties.getBaseUrl()).build(), properties);
    }

    @Bean
    @ConditionalOnMissingBean
    IdentityAccessGuard identityAccessGuard(IdentityVerifier identityVerifier, IdentityTrustProperties properties) {
        return new IdentityAccessGuard(identityVerifier, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    RiskEvaluator riskEvaluator(RiskTrustProperties properties, ObjectProvider<RestClient.Builder> builder) {
        if (!properties.isEnabled()) {
            return RiskEvaluator.noop();
        }
        return new RemoteRiskEvaluator(
                builder.getIfAvailable(RestClient::builder).baseUrl(properties.getBaseUrl()).build(), properties);
    }

    @Bean
    @ConditionalOnMissingBean
    RiskGuard riskGuard(RiskEvaluator riskEvaluator, RiskTrustProperties properties) {
        return new RiskGuard(riskEvaluator, properties);
    }
}
