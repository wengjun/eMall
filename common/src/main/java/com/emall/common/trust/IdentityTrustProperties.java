package com.emall.common.trust;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "emall.trust.identity")
public class IdentityTrustProperties {
    private boolean enabled;
    private boolean failClosed = true;
    private String baseUrl = "http://identity:8080";
}
