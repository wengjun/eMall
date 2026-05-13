package com.emall.risk;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    @Select("""
            SELECT rule_id, scene, rule_code, field_name, operator, threshold_value, risk_level, status, created_at,
                updated_at
            FROM risk_rule
            WHERE rule_id = #{ruleId}
            """)
    RiskRule findRule(@Param("ruleId") long ruleId);

    @Select("""
            SELECT rule_id, scene, rule_code, field_name, operator, threshold_value, risk_level, status, created_at,
                updated_at
            FROM risk_rule
            WHERE scene = #{scene} AND status = 'ACTIVE'
            ORDER BY updated_at DESC
            """)
    List<RiskRule> findActiveRules(@Param("scene") RiskScene scene);

    @Insert("""
            INSERT INTO risk_device_reputation (device_id, reputation_score, risky, updated_at)
            VALUES (#{reputation.deviceId}, #{reputation.reputationScore}, #{reputation.risky},
                #{reputation.updatedAt})
            ON DUPLICATE KEY UPDATE reputation_score = VALUES(reputation_score), risky = VALUES(risky),
                updated_at = VALUES(updated_at)
            """)
    int saveDevice(@Param("reputation") DeviceReputation reputation);

    @Select("""
            SELECT device_id, reputation_score, risky, updated_at
            FROM risk_device_reputation
            WHERE device_id = #{deviceId}
            """)
    DeviceReputation findDevice(@Param("deviceId") String deviceId);

    @Insert("""
            INSERT INTO risk_event
                (event_id, scene, subject_id, device_id, ip, amount, velocity, score, risk_level, reason,
                occurred_at)
            VALUES (#{event.eventId}, #{event.scene}, #{event.subjectId}, #{event.deviceId}, #{event.ip},
                #{event.amount}, #{event.velocity}, #{event.score}, #{event.level}, #{event.reason},
                #{event.occurredAt})
            """)
    int saveEvent(@Param("event") RiskEvent event);

    @Select("""
            SELECT event_id, scene, subject_id, device_id, ip, amount, velocity, score, risk_level, reason,
                occurred_at
            FROM risk_event
            WHERE subject_id = #{subjectId}
            ORDER BY occurred_at DESC
            """)
    List<RiskEvent> findEvents(@Param("subjectId") String subjectId);
}
