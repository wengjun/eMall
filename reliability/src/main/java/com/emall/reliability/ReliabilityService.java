package com.emall.reliability;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReliabilityService {
    private final ReliabilityRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    ReliabilityService(ReliabilityRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    CapacityRehearsal createRehearsal(String serviceName, int targetQps, int peakConcurrency) {
        Instant now = Instant.now();
        return repository.saveRehearsal(new CapacityRehearsal(idGenerator.nextId(), normalize(serviceName), targetQps,
                peakConcurrency, GateStatus.OPEN, now, now));
    }

    @Transactional
    CapacityRehearsal changeRehearsalStatus(long rehearsalId, GateStatus status) {
        CapacityRehearsal rehearsal = repository.findRehearsal(rehearsalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "capacity rehearsal not found"));
        return repository.saveRehearsal(rehearsal.changeStatus(status));
    }

    @Transactional
    SloObjective defineSlo(String serviceName, BigDecimal availabilityTarget, int latencyP95Ms,
            BigDecimal errorBudgetPercent) {
        return repository.saveSlo(new SloObjective(idGenerator.nextId(), normalize(serviceName), availabilityTarget,
                latencyP95Ms, errorBudgetPercent, Instant.now()));
    }

    @Transactional
    ChaosSchedule scheduleChaos(String serviceName, String drillType, int blastRadiusPercent, Instant scheduledAt) {
        if (blastRadiusPercent < 0 || blastRadiusPercent > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "blast radius must be 0-100");
        }
        return repository.saveChaos(new ChaosSchedule(idGenerator.nextId(), normalize(serviceName),
                normalize(drillType), blastRadiusPercent, GateStatus.OPEN, scheduledAt, Instant.now()));
    }

    @Transactional
    ChaosSchedule approveChaos(long chaosId) {
        ChaosSchedule chaos = repository.findChaos(chaosId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "chaos schedule not found"));
        return repository.saveChaos(chaos.approve());
    }

    @Transactional
    ReadinessGate evaluateReadiness(String serviceName, boolean runbookReady, boolean dashboardReady,
            boolean rollbackReady) {
        GateStatus status = runbookReady && dashboardReady && rollbackReady ? GateStatus.PASSED : GateStatus.BLOCKED;
        Instant now = Instant.now();
        return repository.saveReadinessGate(new ReadinessGate(idGenerator.nextId(), normalize(serviceName),
                runbookReady, dashboardReady, rollbackReady, status, now, now));
    }

    ReliabilitySummary summary() {
        int approvedChaos = (int) repository.findChaosSchedules().stream()
                .filter(chaos -> chaos.approvalStatus() == GateStatus.PASSED).count();
        int blockedGates = (int) repository.findReadinessGates().stream()
                .filter(gate -> gate.status() == GateStatus.BLOCKED).count();
        return new ReliabilitySummary(repository.findRehearsals().size(), approvedChaos, blockedGates,
                repository.findSlos().size());
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "reliability value must not be blank");
        }
        return normalized;
    }
}
