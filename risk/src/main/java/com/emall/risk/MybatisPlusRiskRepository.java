package com.emall.risk;

import java.util.List;
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
        return Optional.ofNullable(riskMapper.findRule(ruleId));
    }

    @Override
    public List<RiskRule> findActiveRules(RiskScene scene) {
        return riskMapper.findActiveRules(scene);
    }

    @Override
    public DeviceReputation saveDevice(DeviceReputation reputation) {
        riskMapper.saveDevice(reputation);
        return reputation;
    }

    @Override
    public Optional<DeviceReputation> findDevice(String deviceId) {
        return Optional.ofNullable(riskMapper.findDevice(deviceId));
    }

    @Override
    public RiskEvent saveEvent(RiskEvent event) {
        riskMapper.saveEvent(event);
        return event;
    }

    @Override
    public List<RiskEvent> findEvents(String subjectId) {
        return riskMapper.findEvents(subjectId);
    }
}
