package com.emall.risk;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RiskService {
    private final RiskRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    RiskService(RiskRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    RiskRule createRule(RiskScene scene, String ruleCode, String fieldName, RuleOperator operator, BigDecimal threshold,
            RiskLevel level) {
        if (threshold.signum() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "risk threshold must not be negative");
        }
        Instant now = Instant.now();
        return repository.saveRule(new RiskRule(idGenerator.nextId(), scene, normalize(ruleCode), normalize(fieldName),
                operator, threshold, level, RuleStatus.DRAFT, now, now));
    }

    @Transactional
    RiskRule changeRuleStatus(long ruleId, RuleStatus status) {
        RiskRule rule = repository.findRule(ruleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "risk rule not found"));
        return repository.saveRule(rule.changeStatus(status));
    }

    @Transactional
    DeviceReputation upsertDevice(String deviceId, int reputationScore, boolean risky) {
        if (reputationScore < 0 || reputationScore > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "device reputation score must be 0-100");
        }
        return repository.saveDevice(new DeviceReputation(normalize(deviceId), reputationScore, risky, Instant.now()));
    }

    @Transactional
    RiskDecision evaluate(RiskScene scene, String subjectId, String deviceId, String ip, BigDecimal amount,
            int velocity) {
        String normalizedDeviceId = normalize(deviceId);
        int deviceScore = repository.findDevice(normalizedDeviceId)
                .map(device -> device.risky() ? Math.max(device.reputationScore(), 80) : device.reputationScore())
                .orElse(0);
        RuleMatch match =
                repository.findActiveRules(scene).stream().filter(rule -> matches(rule, amount, velocity, deviceScore))
                        .map(rule -> new RuleMatch(rule.level(), scoreFor(rule.level()), rule.ruleCode()))
                        .max(Comparator.comparingInt(RuleMatch::score)).orElse(defaultDecision(deviceScore));
        RiskDecision decision = new RiskDecision(match.level(), Math.max(match.score(), deviceScore), match.reason());
        repository.saveEvent(new RiskEvent(idGenerator.nextId(), scene, normalize(subjectId), normalizedDeviceId,
                normalize(ip), amount, velocity, decision.score(), decision.level(), decision.reason(), Instant.now()));
        return decision;
    }

    List<RiskEvent> findEvents(String subjectId) {
        return repository.findEvents(normalize(subjectId));
    }

    private boolean matches(RiskRule rule, BigDecimal amount, int velocity, int deviceScore) {
        BigDecimal value = switch (rule.fieldName()) {
            case "amount" -> amount;
            case "velocity" -> BigDecimal.valueOf(velocity);
            case "device_reputation" -> BigDecimal.valueOf(deviceScore);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "unsupported risk rule field");
        };
        int compared = value.compareTo(rule.threshold());
        return switch (rule.operator()) {
            case GREATER_THAN -> compared > 0;
            case LESS_THAN -> compared < 0;
            case EQUALS -> compared == 0;
        };
    }

    private int scoreFor(RiskLevel level) {
        return switch (level) {
            case PASS -> 0;
            case REVIEW -> 60;
            case BLOCK -> 100;
        };
    }

    private RuleMatch defaultDecision(int deviceScore) {
        if (deviceScore >= 80) {
            return new RuleMatch(RiskLevel.REVIEW, deviceScore, "risky-device");
        }
        return new RuleMatch(RiskLevel.PASS, deviceScore, "default-pass");
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "risk value must not be blank");
        }
        return normalized;
    }

    private record RuleMatch(RiskLevel level, int score, String reason) {
    }
}
