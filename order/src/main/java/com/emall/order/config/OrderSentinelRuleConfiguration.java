package com.emall.order.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.emall.common.resilience.SentinelRuleProperties;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.cloud.sentinel.enabled", havingValue = "true")
@EnableConfigurationProperties(SentinelRuleProperties.class)
@RefreshScope
public class OrderSentinelRuleConfiguration {
    private final SentinelRuleProperties properties;

    public OrderSentinelRuleConfiguration(SentinelRuleProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void loadRules() {
        FlowRuleManager.loadRules(List.of(flow("order.inventory.reserve", 5000), flow("order.inventory.confirm", 5000),
                flow("order.inventory.release", 5000), flow("order.pricing.quote", 5000),
                flow("order.marketing.quote", 3000)));
        DegradeRuleManager.loadRules(List.of(degrade("order.inventory.reserve", 0.5, 30, 50),
                degrade("order.inventory.confirm", 0.5, 30, 50), degrade("order.inventory.release", 0.5, 30, 50),
                degrade("order.pricing.quote", 0.5, 10, 20), degrade("order.marketing.quote", 0.5, 10, 20)));
    }

    private FlowRule flow(String resource, double qps) {
        SentinelRuleProperties.FlowRuleSetting setting = properties.getFlows().get(resource);
        FlowRule rule = new FlowRule(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(setting == null || setting.getQps() <= 0 ? qps : setting.getQps());
        return rule;
    }

    private DegradeRule degrade(String resource, double exceptionRatio, int timeWindowSeconds, int minRequestAmount) {
        SentinelRuleProperties.DegradeRuleSetting setting = properties.getDegrades().get(resource);
        DegradeRule rule = new DegradeRule(resource);
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        rule.setCount(
                setting == null || setting.getExceptionRatio() <= 0 ? exceptionRatio : setting.getExceptionRatio());
        rule.setTimeWindow(setting == null || setting.getTimeWindowSeconds() <= 0
                ? timeWindowSeconds
                : setting.getTimeWindowSeconds());
        rule.setMinRequestAmount(setting == null || setting.getMinRequestAmount() <= 0
                ? minRequestAmount
                : setting.getMinRequestAmount());
        rule.setStatIntervalMs(10_000);
        return rule;
    }
}
