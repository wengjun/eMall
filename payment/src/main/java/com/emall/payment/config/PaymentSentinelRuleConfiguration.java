package com.emall.payment.config;

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
public class PaymentSentinelRuleConfiguration {
    private final SentinelRuleProperties properties;

    public PaymentSentinelRuleConfiguration(SentinelRuleProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void loadRules() {
        FlowRuleManager.loadRules(List.of(flow("payment.order.pay", 5000)));
        DegradeRuleManager.loadRules(List.of(degrade("payment.order.pay", 0.5, 30, 20)));
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
