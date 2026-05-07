package com.emall.risk;

import java.math.BigDecimal;
import java.time.Instant;

enum RiskScene {
    ACCOUNT_LOGIN,
    REGISTRATION,
    ORDER_CREATE,
    COUPON_CLAIM,
    PAYMENT,
    REFUND,
    FLASH_SALE_TOKEN,
    SEARCH
}

enum RiskLevel {
    PASS,
    REVIEW,
    BLOCK
}

enum RuleStatus {
    DRAFT,
    ACTIVE,
    DISABLED
}

enum RuleOperator {
    GREATER_THAN,
    LESS_THAN,
    EQUALS
}

record RiskRule(long ruleId, RiskScene scene, String ruleCode, String fieldName, RuleOperator operator,
        BigDecimal threshold, RiskLevel level, RuleStatus status, Instant createdAt, Instant updatedAt) {
    RiskRule changeStatus(RuleStatus nextStatus) {
        return new RiskRule(ruleId, scene, ruleCode, fieldName, operator, threshold, level, nextStatus, createdAt,
                Instant.now());
    }
}

record DeviceReputation(String deviceId, int reputationScore, boolean risky, Instant updatedAt) {
}

record RiskEvent(long eventId, RiskScene scene, String subjectId, String deviceId, String ip, BigDecimal amount,
        int velocity, int score, RiskLevel level, String reason, Instant occurredAt) {
}

record RiskDecision(RiskLevel level, int score, String reason) {
}
