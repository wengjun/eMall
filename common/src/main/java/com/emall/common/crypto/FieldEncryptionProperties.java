package com.emall.common.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emall.security.field-encryption")
public class FieldEncryptionProperties {
    private String key = "local-development-field-encryption-key";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
