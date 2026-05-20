package com.emall.common.trust;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;

public class RiskGuard {
    private final RiskEvaluator riskEvaluator;
    private final RiskTrustProperties properties;

    public RiskGuard(RiskEvaluator riskEvaluator, RiskTrustProperties properties) {
        this.riskEvaluator = riskEvaluator;
        this.properties = properties;
    }

    public static RiskGuard noop() {
        return new RiskGuard(RiskEvaluator.noop(), new RiskTrustProperties());
    }

    public RiskDecision check(RiskEvaluationRequest request) {
        if (!properties.isEnabled()) {
            return RiskDecision.pass();
        }
        RiskDecision decision = riskEvaluator.evaluate(request);
        if (decision.level() == RiskLevel.BLOCK
                || properties.isBlockReviewDecisions() && decision.level() == RiskLevel.REVIEW) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "risk decision blocked: " + decision.reason());
        }
        return decision;
    }
}
