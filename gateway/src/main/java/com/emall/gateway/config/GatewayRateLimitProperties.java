package com.emall.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties(prefix = "emall.gateway.rate-limit")
@Data
public class GatewayRateLimitProperties {
    private boolean includeClientType = true;
    private boolean includeChannel = true;
    private boolean includeDevice = true;
    private boolean includeSku = true;
    private boolean includeCampaign = true;
    private boolean includeRegionCell = true;
    private boolean includeIp = true;
}
