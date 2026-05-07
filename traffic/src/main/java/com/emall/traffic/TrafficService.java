package com.emall.traffic;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TrafficService {
    private final TrafficRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    TrafficService(TrafficRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    UnitCell registerUnit(String unitCode, String regionCode, int capacityWeight) {
        if (capacityWeight <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "capacity weight must be positive");
        }
        Instant now = Instant.now();
        return repository.saveUnit(new UnitCell(idGenerator.nextId(), normalize(unitCode), normalize(regionCode),
                capacityWeight, UnitStatus.ACTIVE, now, now));
    }

    @Transactional
    ShardRoute routeShard(String domainName, int shardNo, String unitCode, String databaseKey) {
        requireUnit(unitCode);
        return repository.saveRoute(new ShardRoute(idGenerator.nextId(), normalize(domainName), shardNo,
                normalize(unitCode), normalize(databaseKey), Instant.now()));
    }

    @Transactional
    TrafficShift planShift(String sourceUnit, String targetUnit, int percent, String reason) {
        requireUnit(sourceUnit);
        requireUnit(targetUnit);
        if (percent < 0 || percent > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "traffic percent must be 0-100");
        }
        Instant now = Instant.now();
        return repository.saveShift(new TrafficShift(idGenerator.nextId(), normalize(sourceUnit), normalize(targetUnit),
                percent, ShiftStatus.PLANNED, reason, now, now));
    }

    @Transactional
    TrafficShift changeShiftStatus(long shiftId, ShiftStatus status) {
        TrafficShift shift = repository.findShift(shiftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "traffic shift not found"));
        return repository.saveShift(shift.changeStatus(status));
    }

    @Transactional
    UnitCell isolateUnit(String unitCode) {
        return repository.saveUnit(requireUnit(unitCode).changeStatus(UnitStatus.ISOLATED));
    }

    TrafficSummary summary() {
        int active = (int) repository.findUnits().stream().filter(unit -> unit.status() == UnitStatus.ACTIVE).count();
        int isolated =
                (int) repository.findUnits().stream().filter(unit -> unit.status() == UnitStatus.ISOLATED).count();
        int running =
                (int) repository.findShifts().stream().filter(shift -> shift.status() == ShiftStatus.RUNNING).count();
        return new TrafficSummary(active, repository.findRoutes().size(), running, isolated);
    }

    private UnitCell requireUnit(String unitCode) {
        return repository.findUnit(normalize(unitCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "unit cell not found"));
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "traffic value must not be blank");
        }
        return normalized;
    }
}
