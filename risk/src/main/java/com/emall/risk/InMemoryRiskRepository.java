package com.emall.risk;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryRiskRepository implements RiskRepository {
    private final ConcurrentMap<Long, RiskRule> rules = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DeviceReputation> devices = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, RiskEvent> events = new ConcurrentHashMap<>();

    @Override
    public RiskRule saveRule(RiskRule rule) {
        rules.put(rule.ruleId(), rule);
        return rule;
    }

    @Override
    public Optional<RiskRule> findRule(long ruleId) {
        return Optional.ofNullable(rules.get(ruleId));
    }

    @Override
    public List<RiskRule> findActiveRules(RiskScene scene) {
        return rules.values().stream()
                .filter(rule -> rule.scene() == scene)
                .filter(rule -> rule.status() == RuleStatus.ACTIVE)
                .sorted(Comparator.comparing(RiskRule::updatedAt).reversed())
                .toList();
    }

    @Override
    public DeviceReputation saveDevice(DeviceReputation reputation) {
        devices.put(reputation.deviceId(), reputation);
        return reputation;
    }

    @Override
    public Optional<DeviceReputation> findDevice(String deviceId) {
        return Optional.ofNullable(devices.get(deviceId));
    }

    @Override
    public RiskEvent saveEvent(RiskEvent event) {
        events.put(event.eventId(), event);
        return event;
    }

    @Override
    public List<RiskEvent> findEvents(String subjectId) {
        return events.values().stream()
                .filter(event -> event.subjectId().equals(subjectId))
                .sorted(Comparator.comparing(RiskEvent::occurredAt).reversed())
                .toList();
    }
}
