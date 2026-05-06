package com.emall.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RiskServiceTest {
    private final InMemoryRiskRepository repository = new InMemoryRiskRepository();
    private final RiskService service = new RiskService(repository, new SnowflakeIdGenerator(22L));

    @Test
    void blocksHighVelocityCouponClaims() {
        RiskRule rule = service.createRule(RiskScene.COUPON_CLAIM, "coupon-velocity", "velocity",
                RuleOperator.GREATER_THAN, new BigDecimal("20"), RiskLevel.BLOCK);
        service.changeRuleStatus(rule.ruleId(), RuleStatus.ACTIVE);

        RiskDecision decision = service.evaluate(RiskScene.COUPON_CLAIM, "user-1", "device-1", "127.0.0.1",
                BigDecimal.ZERO, 30);

        assertThat(decision.level()).isEqualTo(RiskLevel.BLOCK);
        assertThat(service.findEvents("user-1")).hasSize(1);
    }

    @Test
    void reviewsRiskyDeviceEvenWithoutMatchingRule() {
        service.upsertDevice("device-1", 85, true);

        RiskDecision decision = service.evaluate(RiskScene.ACCOUNT_LOGIN, "user-1", "device-1", "127.0.0.1",
                BigDecimal.ZERO, 1);

        assertThat(decision.score()).isEqualTo(85);
        assertThat(decision.level()).isEqualTo(RiskLevel.REVIEW);
    }
}
