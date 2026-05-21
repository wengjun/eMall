package com.emall.risk;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface RiskMapper {
    @Insert("""
            INSERT INTO risk_rule
                (rule_id, scene, rule_code, field_name, operator, threshold_value, risk_level, status,
                created_at, updated_at)
            VALUES (#{rule.ruleId}, #{rule.scene}, #{rule.ruleCode}, #{rule.fieldName}, #{rule.operator},
                #{rule.threshold}, #{rule.level}, #{rule.status}, #{rule.createdAt}, #{rule.updatedAt})
            ON DUPLICATE KEY UPDATE field_name = VALUES(field_name), operator = VALUES(operator),
                threshold_value = VALUES(threshold_value), risk_level = VALUES(risk_level),
                status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveRule(@Param("rule") RiskRule rule);

    @Insert("""
            INSERT INTO risk_device_reputation (device_id, reputation_score, risky, updated_at)
            VALUES (#{reputation.deviceId}, #{reputation.reputationScore}, #{reputation.risky},
                #{reputation.updatedAt})
            ON DUPLICATE KEY UPDATE reputation_score = VALUES(reputation_score), risky = VALUES(risky),
                updated_at = VALUES(updated_at)
            """)
    int saveDevice(@Param("reputation") DeviceReputation reputation);
}
