package com.emall.risk;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcRiskRepository implements RiskRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcRiskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RiskRule saveRule(RiskRule rule) {
        jdbcTemplate.update("""
                INSERT INTO risk_rule
                    (rule_id, scene, rule_code, field_name, operator, threshold_value, risk_level, status,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE field_name = VALUES(field_name), operator = VALUES(operator),
                    threshold_value = VALUES(threshold_value), risk_level = VALUES(risk_level),
                    status = VALUES(status), updated_at = VALUES(updated_at)
                """, rule.ruleId(), rule.scene().name(), rule.ruleCode(), rule.fieldName(), rule.operator().name(),
                rule.threshold(), rule.level().name(), rule.status().name(), Timestamp.from(rule.createdAt()),
                Timestamp.from(rule.updatedAt()));
        return rule;
    }

    @Override
    public Optional<RiskRule> findRule(long ruleId) {
        return jdbcTemplate.query("SELECT * FROM risk_rule WHERE rule_id = ?", this::mapRule, ruleId)
                .stream().findFirst();
    }

    @Override
    public List<RiskRule> findActiveRules(RiskScene scene) {
        return jdbcTemplate.query("""
                SELECT * FROM risk_rule
                WHERE scene = ? AND status = 'ACTIVE'
                ORDER BY updated_at DESC
                """, this::mapRule, scene.name());
    }

    @Override
    public DeviceReputation saveDevice(DeviceReputation reputation) {
        jdbcTemplate.update("""
                INSERT INTO risk_device_reputation (device_id, reputation_score, risky, updated_at)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE reputation_score = VALUES(reputation_score), risky = VALUES(risky),
                    updated_at = VALUES(updated_at)
                """, reputation.deviceId(), reputation.reputationScore(), reputation.risky(),
                Timestamp.from(reputation.updatedAt()));
        return reputation;
    }

    @Override
    public Optional<DeviceReputation> findDevice(String deviceId) {
        return jdbcTemplate.query("SELECT * FROM risk_device_reputation WHERE device_id = ?",
                this::mapDevice, deviceId).stream().findFirst();
    }

    @Override
    public RiskEvent saveEvent(RiskEvent event) {
        jdbcTemplate.update("""
                INSERT INTO risk_event
                    (event_id, scene, subject_id, device_id, ip, amount, velocity, score, risk_level, reason,
                    occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, event.eventId(), event.scene().name(), event.subjectId(), event.deviceId(), event.ip(),
                event.amount(), event.velocity(), event.score(), event.level().name(), event.reason(),
                Timestamp.from(event.occurredAt()));
        return event;
    }

    @Override
    public List<RiskEvent> findEvents(String subjectId) {
        return jdbcTemplate.query("""
                SELECT * FROM risk_event
                WHERE subject_id = ?
                ORDER BY occurred_at DESC
                """, this::mapEvent, subjectId);
    }

    private RiskRule mapRule(ResultSet rs, int rowNum) throws SQLException {
        return new RiskRule(rs.getLong("rule_id"), RiskScene.valueOf(rs.getString("scene")),
                rs.getString("rule_code"), rs.getString("field_name"),
                RuleOperator.valueOf(rs.getString("operator")), rs.getBigDecimal("threshold_value"),
                RiskLevel.valueOf(rs.getString("risk_level")), RuleStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private DeviceReputation mapDevice(ResultSet rs, int rowNum) throws SQLException {
        return new DeviceReputation(rs.getString("device_id"), rs.getInt("reputation_score"),
                rs.getBoolean("risky"), rs.getTimestamp("updated_at").toInstant());
    }

    private RiskEvent mapEvent(ResultSet rs, int rowNum) throws SQLException {
        return new RiskEvent(rs.getLong("event_id"), RiskScene.valueOf(rs.getString("scene")),
                rs.getString("subject_id"), rs.getString("device_id"), rs.getString("ip"),
                rs.getBigDecimal("amount"), rs.getInt("velocity"), rs.getInt("score"),
                RiskLevel.valueOf(rs.getString("risk_level")), rs.getString("reason"),
                rs.getTimestamp("occurred_at").toInstant());
    }
}
