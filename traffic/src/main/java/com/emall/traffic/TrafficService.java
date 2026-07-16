package com.emall.traffic;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.controlplane.ControlPlaneAssertions;
import com.emall.common.controlplane.ControlPlaneClient;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TrafficService {
    private final TrafficRepository repository;
    private final SnowflakeIdGenerator idGenerator;
    private final TrafficControlPlanePublisher controlPlanePublisher;
    private final ControlPlaneClient controlPlaneClient;

    TrafficService(TrafficRepository repository, SnowflakeIdGenerator idGenerator,
            TrafficControlPlanePublisher controlPlanePublisher, ControlPlaneClient controlPlaneClient) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.controlPlanePublisher = controlPlanePublisher;
        this.controlPlaneClient = controlPlaneClient;
    }

    @Transactional
    UnitCell registerUnit(String unitCode, String regionCode, int capacityWeight) {
        if (capacityWeight <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "capacity weight must be positive");
        }
        Instant now = Instant.now();
        UnitCell unit = repository.saveUnit(new UnitCell(idGenerator.nextId(), normalize(unitCode),
                normalize(regionCode), capacityWeight, UnitStatus.ACTIVE, now, now));
        publishRouting();
        return unit;
    }

    @Transactional
    ShardRoute routeShard(String domainName, int shardNo, String unitCode, String databaseKey) {
        requireUnit(unitCode);
        ShardRoute route = repository.saveRoute(new ShardRoute(idGenerator.nextId(), normalize(domainName), shardNo,
                normalize(unitCode), normalize(databaseKey), Instant.now()));
        publishRouting();
        return route;
    }

    @Transactional
    TrafficShift planShift(String sourceUnit, String targetUnit, int percent, String reason) {
        requireUnit(sourceUnit);
        requireUnit(targetUnit);
        if (percent < 0 || percent > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "traffic percent must be 0-100");
        }
        Instant now = Instant.now();
        TrafficShift shift = repository.saveShift(new TrafficShift(idGenerator.nextId(), normalize(sourceUnit),
                normalize(targetUnit), percent, ShiftStatus.PLANNED, reason, now, now));
        publishRouting();
        return shift;
    }

    @Transactional
    TrafficShift changeShiftStatus(long shiftId, ShiftStatus status) {
        TrafficShift shift = repository.findShift(shiftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "traffic shift not found"));
        if (status == ShiftStatus.COMPLETED) {
            ControlPlaneAssertions.requireSucceeded(controlPlaneClient, "traffic", "global-traffic-policy",
                    "multi-region");
        }
        TrafficShift saved = repository.saveShift(shift.changeStatus(status));
        publishRouting();
        return saved;
    }

    @Transactional
    UnitCell isolateUnit(String unitCode) {
        UnitCell unit = repository.saveUnit(requireUnit(unitCode).changeStatus(UnitStatus.ISOLATED));
        publishRouting();
        return unit;
    }

    @Transactional
    TrafficControlRule upsertControlRule(String resource, ControlRuleType type, String dimension, String matchValue,
            int threshold, String unitCode, boolean enabled) {
        if (threshold <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "control threshold must be positive");
        }
        requireUnit(unitCode);
        Instant now = Instant.now();
        TrafficControlRule rule = repository.saveControlRule(
                new TrafficControlRule(idGenerator.nextId(), normalize(resource), type, normalize(dimension),
                        normalize(matchValue), threshold, normalize(unitCode), enabled, now, now));
        controlPlanePublisher.publishControlRules(repository.findControlRules());
        return rule;
    }

    @Transactional
    TrafficControlRule changeControlRule(long ruleId, boolean enabled) {
        TrafficControlRule rule = repository.findControlRule(ruleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "traffic control rule not found"));
        TrafficControlRule saved = repository.saveControlRule(rule.changeEnabled(enabled));
        controlPlanePublisher.publishControlRules(repository.findControlRules());
        return saved;
    }

    java.util.List<TrafficControlRule> controlRules() {
        return repository.findControlRules();
    }

    TrafficSummary summary() {
        int active = (int) repository.findUnits().stream().filter(unit -> unit.status() == UnitStatus.ACTIVE).count();
        int isolated =
                (int) repository.findUnits().stream().filter(unit -> unit.status() == UnitStatus.ISOLATED).count();
        int running =
                (int) repository.findShifts().stream().filter(shift -> shift.status() == ShiftStatus.RUNNING).count();
        return new TrafficSummary(active, repository.findRoutes().size(), running, isolated,
                repository.findControlRules().size());
    }

    private UnitCell requireUnit(String unitCode) {
        return repository.findUnit(normalize(unitCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "unit cell not found"));
    }

    private void publishRouting() {
        controlPlanePublisher.publishRouting(repository.findUnits(), repository.findRoutes(), repository.findShifts());
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "traffic value must not be blank");
        }
        return normalized;
    }
}
