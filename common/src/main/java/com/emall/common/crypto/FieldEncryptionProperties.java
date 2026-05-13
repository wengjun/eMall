package com.emall.common.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "emall.security.field-encryption")
@Getter
@Setter
public class FieldEncryptionProperties {
    private String key = "local-development-field-encryption-key";
}
