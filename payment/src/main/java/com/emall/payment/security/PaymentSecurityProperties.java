package com.emall.payment.security;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emall.payment.security")
public class PaymentSecurityProperties {
    private boolean callbackSignatureEnabled = true;
    private Map<String, String> callbackSecrets = new HashMap<>();
    private long callbackAllowedSkewSeconds = 300;
    private boolean replayStoreFailClosed;

    public boolean isCallbackSignatureEnabled() {
        return callbackSignatureEnabled;
    }

    public void setCallbackSignatureEnabled(boolean callbackSignatureEnabled) {
        this.callbackSignatureEnabled = callbackSignatureEnabled;
    }

    public Map<String, String> getCallbackSecrets() {
        return callbackSecrets;
    }

    public void setCallbackSecrets(Map<String, String> callbackSecrets) {
        this.callbackSecrets = new HashMap<>(callbackSecrets);
    }

    public long getCallbackAllowedSkewSeconds() {
        return callbackAllowedSkewSeconds;
    }

    public void setCallbackAllowedSkewSeconds(long callbackAllowedSkewSeconds) {
        this.callbackAllowedSkewSeconds = callbackAllowedSkewSeconds;
    }

    public boolean isReplayStoreFailClosed() {
        return replayStoreFailClosed;
    }

    public void setReplayStoreFailClosed(boolean replayStoreFailClosed) {
        this.replayStoreFailClosed = replayStoreFailClosed;
    }

    String callbackSecret(String channel) {
        String secret = callbackSecrets.get(channel);
        if (secret == null || secret.isBlank()) {
            secret = callbackSecrets.get("default");
        }
        return secret;
    }
}
