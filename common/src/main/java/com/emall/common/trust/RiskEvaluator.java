package com.emall.common.trust;

@FunctionalInterface
public interface RiskEvaluator {
    RiskDecision evaluate(RiskEvaluationRequest request);

    static RiskEvaluator noop() {
        return request -> RiskDecision.pass();
    }
}
