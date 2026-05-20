package com.emall.common.resilience;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties(prefix = "emall.resilience.sentinel")
@Data
public class SentinelRuleProperties {
    private Map<String, FlowRuleSetting> flows = new LinkedHashMap<>();
    private Map<String, DegradeRuleSetting> degrades = new LinkedHashMap<>();

    @Data
    public static class FlowRuleSetting {
        private double qps;
    }

    @Data
    public static class DegradeRuleSetting {
        private double exceptionRatio;
        private int timeWindowSeconds;
        private int minRequestAmount;
    }
}
