package com.emall.risk;

import static com.emall.common.persistence.RowMaps.booleanValue;
import static com.emall.common.persistence.RowMaps.decimalValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusRiskRepository implements RiskRepository {
    private final RiskMapper riskMapper;

    MybatisPlusRiskRepository(RiskMapper riskMapper) {
        this.riskMapper = riskMapper;
    }

    @Override
    public RiskRule saveRule(RiskRule rule) {
        riskMapper.saveRule(rule);
        return rule;
    }

    @Override
    public Optional<RiskRule> findRule(long ruleId) {
        return Optional.ofNullable(riskMapper.findRule(ruleId)).map(this::mapRule);
    }

    @Override
    public List<RiskRule> findActiveRules(RiskScene scene) {
        return riskMapper.findActiveRules(scene).stream().map(this::mapRule).toList();
    }

    @Override
    public DeviceReputation saveDevice(DeviceReputation reputation) {
        riskMapper.saveDevice(reputation);
        return reputation;
    }

    @Override
    public Optional<DeviceReputation> findDevice(String deviceId) {
        return Optional.ofNullable(riskMapper.findDevice(deviceId)).map(this::mapDevice);
    }

    @Override
    public RiskEvent saveEvent(RiskEvent event) {
        riskMapper.saveEvent(event);
        return event;
    }

    @Override
    public List<RiskEvent> findEvents(String subjectId) {
        return riskMapper.findEvents(subjectId).stream().map(this::mapEvent).toList();
    }

    private RiskRule mapRule(Map<String, Object> row) {
        return new RiskRule(longValue(row, "rule_id"), RiskScene.valueOf(stringValue(row, "scene")),
                stringValue(row, "rule_code"), stringValue(row, "field_name"),
                RuleOperator.valueOf(stringValue(row, "operator")), decimalValue(row, "threshold_value"),
                RiskLevel.valueOf(stringValue(row, "risk_level")), RuleStatus.valueOf(stringValue(row, "status")),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private DeviceReputation mapDevice(Map<String, Object> row) {
        return new DeviceReputation(stringValue(row, "device_id"), intValue(row, "reputation_score"),
                booleanValue(row, "risky"), instantValue(row, "updated_at"));
    }

    private RiskEvent mapEvent(Map<String, Object> row) {
        return new RiskEvent(longValue(row, "event_id"), RiskScene.valueOf(stringValue(row, "scene")),
                stringValue(row, "subject_id"), stringValue(row, "device_id"), stringValue(row, "ip"),
                decimalValue(row, "amount"), intValue(row, "velocity"), intValue(row, "score"),
                RiskLevel.valueOf(stringValue(row, "risk_level")), stringValue(row, "reason"),
                instantValue(row, "occurred_at"));
    }
}
