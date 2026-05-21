package com.emall.risk;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusRiskRepository implements RiskRepository {
    private final RiskMapper riskMapper;
    private final RiskRuleMapper ruleMapper;
    private final DeviceReputationMapper deviceMapper;
    private final RiskEventMapper eventMapper;

    MybatisPlusRiskRepository(RiskMapper riskMapper, RiskRuleMapper ruleMapper, DeviceReputationMapper deviceMapper,
            RiskEventMapper eventMapper) {
        this.riskMapper = riskMapper;
        this.ruleMapper = ruleMapper;
        this.deviceMapper = deviceMapper;
        this.eventMapper = eventMapper;
    }

    @Override
    public RiskRule saveRule(RiskRule rule) {
        riskMapper.saveRule(rule);
        return rule;
    }

    @Override
    public Optional<RiskRule> findRule(long ruleId) {
        return Optional.ofNullable(ruleMapper.selectById(ruleId));
    }

    @Override
    public List<RiskRule> findActiveRules(RiskScene scene) {
        QueryWrapper<RiskRule> query = new QueryWrapper<RiskRule>().eq("scene", scene.name())
                .eq("status", RuleStatus.ACTIVE.name()).orderByDesc("updated_at");
        return ruleMapper.selectList(query);
    }

    @Override
    public DeviceReputation saveDevice(DeviceReputation reputation) {
        riskMapper.saveDevice(reputation);
        return reputation;
    }

    @Override
    public Optional<DeviceReputation> findDevice(String deviceId) {
        return Optional.ofNullable(deviceMapper.selectById(deviceId));
    }

    @Override
    public RiskEvent saveEvent(RiskEvent event) {
        eventMapper.insert(event);
        return event;
    }

    @Override
    public List<RiskEvent> findEvents(String subjectId) {
        return eventMapper
                .selectList(new QueryWrapper<RiskEvent>().eq("subject_id", subjectId).orderByDesc("occurred_at"));
    }
}
