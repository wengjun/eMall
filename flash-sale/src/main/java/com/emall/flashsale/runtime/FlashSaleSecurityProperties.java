package com.emall.flashsale.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emall.flash-sale.security")
public class FlashSaleSecurityProperties {
    private String tokenSecret = "local-dev-flash-sale-token-secret";

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }
}
