package com.emall.common.trust;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emall.trust.risk")
@Getter
@Setter
public class RiskTrustProperties {
    private boolean enabled;
    private boolean failClosed = true;
    private boolean blockReviewDecisions;
    private String baseUrl = "http://risk:8080";
}
