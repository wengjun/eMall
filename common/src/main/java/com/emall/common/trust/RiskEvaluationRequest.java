package com.emall.common.trust;

import java.math.BigDecimal;

public record RiskEvaluationRequest(RiskScene scene, String subjectId, String deviceId, String ip, BigDecimal amount,
        int velocity) {
    public RiskEvaluationRequest {
        amount = amount == null ? BigDecimal.ZERO : amount;
        velocity = Math.max(0, velocity);
    }
}
