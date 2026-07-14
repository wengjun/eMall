package com.emall.flashsale.runtime;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "emall.flash-sale.security")
public class FlashSaleSecurityProperties {
    private String tokenSecret = "local-dev-flash-sale-token-secret";
}
