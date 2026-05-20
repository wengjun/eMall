package com.emall.common.trust;

public record RiskDecision(RiskLevel level, int score, String reason) {
    public static RiskDecision pass() {
        return new RiskDecision(RiskLevel.PASS, 0, "risk-disabled");
    }
}
