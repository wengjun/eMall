package com.emall.common.trust;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emall.trust.identity")
@Getter
@Setter
public class IdentityTrustProperties {
    private boolean enabled;
    private boolean failClosed = true;
    private String baseUrl = "http://identity:8080";
}
