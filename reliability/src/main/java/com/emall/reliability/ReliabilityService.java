package com.emall.reliability;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.controlplane.ControlPlaneAssertions;
import com.emall.common.controlplane.ControlPlaneClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReliabilityService {
    private final ReliabilityRepository repository;
    private final SnowflakeIdGenerator idGenerator;
    private final ReliabilityControlPlanePublisher controlPlanePublisher;
    private final ControlPlaneClient controlPlaneClient;

    ReliabilityService(ReliabilityRepository repository, SnowflakeIdGenerator idGenerator,
            ReliabilityControlPlanePublisher controlPlanePublisher, ControlPlaneClient controlPlaneClient) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.controlPlanePublisher = controlPlanePublisher;
        this.controlPlaneClient = controlPlaneClient;
    }

    @Transactional
    CapacityRehearsal createRehearsal(String serviceName, int targetQps, int peakConcurrency) {
        Instant now = Instant.now();
        CapacityRehearsal rehearsal = repository.saveRehearsal(new CapacityRehearsal(idGenerator.nextId(),
                normalize(serviceName), targetQps, peakConcurrency, GateStatus.OPEN, now, now));
        controlPlanePublisher.runCapacityRehearsal(rehearsal);
        return rehearsal;
    }

    @Transactional
    CapacityRehearsal changeRehearsalStatus(long rehearsalId, GateStatus status) {
        CapacityRehearsal rehearsal = repository.findRehearsal(rehearsalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "capacity rehearsal not found"));
        if (status == GateStatus.PASSED) {
            ControlPlaneAssertions.requireSucceeded(controlPlaneClient, "reliability", "capacity-rehearsal",
                    Long.toString(rehearsalId));
        }
        return repository.saveRehearsal(rehearsal.changeStatus(status));
    }

    @Transactional
    SloObjective defineSlo(String serviceName, BigDecimal availabilityTarget, int latencyP95Ms,
            BigDecimal errorBudgetPercent) {
        SloObjective slo = repository.saveSlo(new SloObjective(idGenerator.nextId(), normalize(serviceName),
                availabilityTarget, latencyP95Ms, errorBudgetPercent, Instant.now()));
        controlPlanePublisher.publishSlo(slo);
        return slo;
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
        ChaosSchedule approved = repository.saveChaos(chaos.approve());
        controlPlanePublisher.scheduleChaos(approved);
        return approved;
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
