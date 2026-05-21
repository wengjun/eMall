package com.emall.risk;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

@TableName("risk_rule")
record RiskRule(@TableId(value = "rule_id", type = IdType.INPUT) long ruleId, RiskScene scene, String ruleCode,
        String fieldName, RuleOperator operator, @TableField("threshold_value") BigDecimal threshold,
        @TableField("risk_level") RiskLevel level, RuleStatus status, Instant createdAt, Instant updatedAt) {
    RiskRule changeStatus(RuleStatus nextStatus) {
        return new RiskRule(ruleId, scene, ruleCode, fieldName, operator, threshold, level, nextStatus, createdAt,
                Instant.now());
    }
}

@TableName("risk_device_reputation")
record DeviceReputation(@TableId(value = "device_id", type = IdType.INPUT) String deviceId, int reputationScore,
        boolean risky, Instant updatedAt) {
}

@TableName("risk_event")
record RiskEvent(@TableId(value = "event_id", type = IdType.INPUT) long eventId, RiskScene scene, String subjectId,
        String deviceId, String ip, BigDecimal amount, int velocity, int score,
        @TableField("risk_level") RiskLevel level, String reason, Instant occurredAt) {
}

record RiskDecision(RiskLevel level, int score, String reason) {
}
