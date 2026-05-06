package com.emall.common.crypto;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(FieldEncryptionProperties.class)
public class FieldEncryptionAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    FieldEncryptor fieldEncryptor(FieldEncryptionProperties properties) {
        return new AesGcmFieldEncryptor(properties.getKey());
    }
}
