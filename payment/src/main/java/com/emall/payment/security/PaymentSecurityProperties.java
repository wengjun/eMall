package com.emall.payment.security;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "emall.payment.security")
public class PaymentSecurityProperties {
    private boolean callbackSignatureEnabled = true;
    private Map<String, String> callbackSecrets = new HashMap<>();
    private long callbackAllowedSkewSeconds = 300;
    private boolean replayStoreFailClosed;

    public void setCallbackSecrets(Map<String, String> callbackSecrets) {
        this.callbackSecrets = new HashMap<>(callbackSecrets);
    }

    String callbackSecret(String channel) {
        String secret = callbackSecrets.get(channel);
        if (secret == null || secret.isBlank()) {
            secret = callbackSecrets.get("default");
        }
        return secret;
    }
}
