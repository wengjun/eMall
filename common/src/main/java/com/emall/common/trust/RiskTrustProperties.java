package com.emall.common.trust;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "emall.trust.risk")
public class RiskTrustProperties {
    private boolean enabled;
    private boolean failClosed = true;
    private boolean blockReviewDecisions;
    private String baseUrl = "http://risk:8080";
}
