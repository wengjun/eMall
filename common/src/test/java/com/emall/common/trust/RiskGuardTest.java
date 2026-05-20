package com.emall.common.trust;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RiskGuardTest {
    @Test
    void returnsPassWhenRiskIsDisabled() {
        RiskTrustProperties properties = new RiskTrustProperties();
        RiskGuard guard = new RiskGuard(request -> new RiskDecision(RiskLevel.BLOCK, 100, "blocked"), properties);

        RiskDecision decision = guard.check(
                new RiskEvaluationRequest(RiskScene.ORDER_CREATE, "1001", "device-1", "127.0.0.1", BigDecimal.TEN, 1));

        assertThat(decision.level()).isEqualTo(RiskLevel.PASS);
    }

    @Test
    void blocksWhenRiskDecisionBlocks() {
        RiskTrustProperties properties = new RiskTrustProperties();
        properties.setEnabled(true);
        RiskGuard guard = new RiskGuard(request -> new RiskDecision(RiskLevel.BLOCK, 100, "rule-hit"), properties);

        assertThatThrownBy(() -> guard.check(
                new RiskEvaluationRequest(RiskScene.PAYMENT, "1001", "device-1", "127.0.0.1", BigDecimal.TEN, 1)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("rule-hit");
    }

    @Test
    void blocksReviewDecisionWhenConfigured() {
        RiskTrustProperties properties = new RiskTrustProperties();
        properties.setEnabled(true);
        properties.setBlockReviewDecisions(true);
        RiskGuard guard = new RiskGuard(request -> new RiskDecision(RiskLevel.REVIEW, 80, "manual-review"), properties);

        assertThatThrownBy(() -> guard.check(
                new RiskEvaluationRequest(RiskScene.ORDER_CREATE, "1001", "device-1", "127.0.0.1", BigDecimal.TEN, 1)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("manual-review");
    }
}
