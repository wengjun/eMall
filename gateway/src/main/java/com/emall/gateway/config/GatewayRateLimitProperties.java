package com.emall.gateway.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties(prefix = "emall.gateway.rate-limit")
@Data
public class GatewayRateLimitProperties {
    private boolean includeClientType;
    private boolean includeChannel;
    private boolean includeDevice;
    private boolean includeSku;
    private boolean includeCampaign;
    private boolean includeRegionCell;
    private boolean includeIp = true;
    private boolean includeRoute = true;
    private int maximumComponentLength = 64;
    private int globalReplenishRate = 1_000_000;
    private int globalBurstCapacity = 1_200_000;
    private int subjectReplenishRate = 1_000;
    private int subjectBurstCapacity = 2_000;
    private int hotResourceReplenishRate = 10_000;
    private int hotResourceBurstCapacity = 20_000;
    private List<String> trustedProxyCidrs =
            new ArrayList<>(List.of("127.0.0.0/8", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "::1/128"));
}
