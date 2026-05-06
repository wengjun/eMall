package com.emall.risk;

import java.util.List;
import java.util.Optional;

interface RiskRepository {
    RiskRule saveRule(RiskRule rule);

    Optional<RiskRule> findRule(long ruleId);

    List<RiskRule> findActiveRules(RiskScene scene);

    DeviceReputation saveDevice(DeviceReputation reputation);

    Optional<DeviceReputation> findDevice(String deviceId);

    RiskEvent saveEvent(RiskEvent event);

    List<RiskEvent> findEvents(String subjectId);
}
