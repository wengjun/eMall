package com.emall.payment.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.cloud.sentinel.enabled", havingValue = "true")
public class PaymentSentinelRuleConfiguration {
    @PostConstruct
    void loadRules() {
        FlowRuleManager.loadRules(List.of(flow("payment.order.pay", 5000)));
        DegradeRuleManager.loadRules(List.of(degrade("payment.order.pay", 0.5, 30, 20)));
    }

    private FlowRule flow(String resource, double qps) {
        FlowRule rule = new FlowRule(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        return rule;
    }

    private DegradeRule degrade(String resource, double exceptionRatio, int timeWindowSeconds, int minRequestAmount) {
        DegradeRule rule = new DegradeRule(resource);
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        rule.setCount(exceptionRatio);
        rule.setTimeWindow(timeWindowSeconds);
        rule.setMinRequestAmount(minRequestAmount);
        rule.setStatIntervalMs(10_000);
        return rule;
    }
}
