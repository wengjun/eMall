package com.emall.payment.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emall.payment.security")
public class PaymentSecurityProperties {
    private boolean callbackSignatureEnabled = true;
    private String callbackSecret = "local-dev-payment-callback-secret";
    private long callbackAllowedSkewSeconds = 300;

    public boolean isCallbackSignatureEnabled() {
        return callbackSignatureEnabled;
    }

    public void setCallbackSignatureEnabled(boolean callbackSignatureEnabled) {
        this.callbackSignatureEnabled = callbackSignatureEnabled;
    }

    public String getCallbackSecret() {
        return callbackSecret;
    }

    public void setCallbackSecret(String callbackSecret) {
        this.callbackSecret = callbackSecret;
    }

    public long getCallbackAllowedSkewSeconds() {
        return callbackAllowedSkewSeconds;
    }

    public void setCallbackAllowedSkewSeconds(long callbackAllowedSkewSeconds) {
        this.callbackAllowedSkewSeconds = callbackAllowedSkewSeconds;
    }
}
