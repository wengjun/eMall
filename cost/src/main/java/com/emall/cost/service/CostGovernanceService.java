package com.emall.cost.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.cost.domain.CapacityRiskLevel;
import com.emall.cost.domain.CapacitySummary;
import com.emall.cost.domain.CostActionStatus;
import com.emall.cost.domain.CostActionType;
import com.emall.cost.domain.CostBudget;
import com.emall.cost.domain.CostOptimizationAction;
import com.emall.cost.domain.CostSignal;
import com.emall.cost.domain.CostSignalType;
import com.emall.cost.domain.CostSummary;
import com.emall.cost.domain.ServiceCapacityBaseline;
import com.emall.cost.repository.CostRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CostGovernanceService {
    private final CostRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    public CostGovernanceService(CostRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public CostSignal recordSignal(String serviceName, CostSignalType signalType, BigDecimal metricValue,
            BigDecimal thresholdValue, Instant observedAt) {
        CostSignal signal = repository.saveSignal(new CostSignal(idGenerator.nextId(), normalizeService(serviceName),
                signalType, normalize(metricValue), normalize(thresholdValue), observedAt, Instant.now()));
        if (breaches(signal)) {
            openAction(signal);
        }
        return signal;
    }

    public List<CostSignal> findSignals(String serviceName, int limit) {
        return repository.findSignalsByService(normalizeService(serviceName), Math.max(1, Math.min(limit, 200)));
    }

    @Transactional
    public CostBudget upsertBudget(String serviceName, BigDecimal monthlyBudget, BigDecimal currentSpend,
            String currency, int alertThresholdPercent, boolean active) {
        if (alertThresholdPercent < 1 || alertThresholdPercent > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "budget alert threshold percent must be 1-100");
        }
        String normalizedService = normalizeService(serviceName);
        String normalizedCurrency = currency.toUpperCase(Locale.ROOT);
        CostBudget budget =
                repository
                        .findActiveBudget(normalizedService).map(existing -> existing.update(normalize(monthlyBudget),
                                normalize(currentSpend), normalizedCurrency, alertThresholdPercent, active))
                        .orElseGet(() -> {
                            Instant now = Instant.now();
                            return new CostBudget(idGenerator.nextId(), normalizedService, normalize(monthlyBudget),
                                    normalize(currentSpend), normalizedCurrency, alertThresholdPercent, active, now,
                                    now);
                        });
        return repository.saveBudget(budget);
    }

    public List<CostOptimizationAction> findActiveActions(String serviceName) {
        return repository.findActiveActionsByService(normalizeService(serviceName));
    }

    @Transactional
    public CostOptimizationAction changeActionStatus(long actionId, CostActionStatus status) {
        CostOptimizationAction action = repository.findAction(actionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "cost optimization action not found"));
        return repository.saveAction(action.changeStatus(status));
    }

    public CostSummary summary(String serviceName) {
        String normalizedService = normalizeService(serviceName);
        CostBudget budget = repository.findActiveBudget(normalizedService).orElse(new CostBudget(0L, normalizedService,
                BigDecimal.ZERO, BigDecimal.ZERO, "USD", 100, false, Instant.EPOCH, Instant.EPOCH));
        return new CostSummary(normalizedService, budget.monthlyBudget(), budget.currentSpend(), budget.currency(),
                budget.alertThresholdPercent(), budget.active() && budget.overAlertThreshold(),
                repository.findSignalsByService(normalizedService, 50).size(),
                repository.findActiveActionsByService(normalizedService).size());
    }

    @Transactional
    public ServiceCapacityBaseline recordCapacityBaseline(String serviceName, int safeQps, int peakQps, int currentQps,
            int currentReplicas, int maxReplicas, BigDecimal cpuUtilization, BigDecimal memoryUtilization,
            BigDecimal monthlyCost, boolean sloProtected, Instant observedAt) {
        validateCapacityInputs(safeQps, peakQps, currentQps, currentReplicas, maxReplicas);
        BigDecimal cpu = utilization(cpuUtilization, "cpu utilization");
        BigDecimal memory = utilization(memoryUtilization, "memory utilization");
        BigDecimal cost = normalize(monthlyCost);
        String normalizedService = normalizeService(serviceName);
        CapacityRiskLevel riskLevel = riskLevel(safeQps, currentQps, currentReplicas, maxReplicas, cpu, memory);
        ServiceCapacityBaseline baseline =
                repository.saveCapacityBaseline(new ServiceCapacityBaseline(idGenerator.nextId(), normalizedService,
                        safeQps, peakQps, currentQps, currentReplicas, maxReplicas, cpu, memory, cost, sloProtected,
                        riskLevel, recommendation(riskLevel, sloProtected), observedAt, Instant.now()));
        openCapacityAction(baseline);
        return baseline;
    }

    public CapacitySummary capacitySummary(String serviceName) {
        String normalizedService = normalizeService(serviceName);
        ServiceCapacityBaseline baseline = repository.findLatestCapacityBaseline(normalizedService)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "capacity baseline not found"));
        return new CapacitySummary(normalizedService, baseline.safeQps(), baseline.currentQps(),
                baseline.qpsWatermark(), baseline.currentReplicas(), baseline.maxReplicas(), baseline.hpaWatermark(),
                baseline.riskLevel(), baseline.recommendation(), baseline.monthlyCost(),
                repository.findActiveActionsByService(normalizedService).size());
    }

    private void openAction(CostSignal signal) {
        CostActionType actionType = actionType(signal.signalType());
        repository.findActiveAction(signal.serviceName(), signal.signalType(), actionType)
                .orElseGet(() -> repository.saveAction(new CostOptimizationAction(idGenerator.nextId(),
                        signal.serviceName(), signal.signalType(), actionType, CostActionStatus.OPEN,
                        priority(signal.signalType()), description(signal), Instant.now(), Instant.now())));
    }

    private void openCapacityAction(ServiceCapacityBaseline baseline) {
        if (baseline.riskLevel() == CapacityRiskLevel.NONE) {
            return;
        }
        CostSignalType signalType = capacitySignalType(baseline.riskLevel());
        CostActionType actionType = capacityActionType(baseline.riskLevel());
        repository.findActiveAction(baseline.serviceName(), signalType, actionType).orElseGet(
                () -> repository.saveAction(new CostOptimizationAction(idGenerator.nextId(), baseline.serviceName(),
                        signalType, actionType, CostActionStatus.OPEN, capacityPriority(baseline.riskLevel()),
                        baseline.recommendation(), Instant.now(), Instant.now())));
    }

    private boolean breaches(CostSignal signal) {
        if (signal.signalType() == CostSignalType.CACHE_HIT_RATIO) {
            return signal.metricValue().compareTo(signal.thresholdValue()) < 0;
        }
        return signal.metricValue().compareTo(signal.thresholdValue()) > 0;
    }

    private CostActionType actionType(CostSignalType signalType) {
        return switch (signalType) {
            case CACHE_HIT_RATIO -> CostActionType.INCREASE_CACHE_TTL;
            case INDEX_STORAGE_GB -> CostActionType.ROLLOVER_INDEX;
            case HOT_STORAGE_GB -> CostActionType.MOVE_TO_COLD_STORAGE;
            case COLD_STORAGE_GB -> CostActionType.TIGHTEN_RETENTION_POLICY;
            case ASYNC_EXPORT_BACKLOG -> CostActionType.THROTTLE_EXPORT;
            case HPA_REPLICA_WATERMARK -> CostActionType.REVIEW_HPA_LIMIT;
            case QPS_WATERMARK -> CostActionType.SCALE_SERVICE_REPLICAS;
            case IDLE_RESOURCE -> CostActionType.REDUCE_IDLE_REPLICAS;
            case OVER_REPLICATED -> CostActionType.RIGHTSIZE_RESOURCE_REQUESTS;
        };
    }

    private int priority(CostSignalType signalType) {
        return switch (signalType) {
            case CACHE_HIT_RATIO, ASYNC_EXPORT_BACKLOG -> 1;
            case HPA_REPLICA_WATERMARK, QPS_WATERMARK -> 1;
            case HOT_STORAGE_GB, INDEX_STORAGE_GB -> 2;
            case IDLE_RESOURCE, OVER_REPLICATED -> 2;
            case COLD_STORAGE_GB -> 3;
        };
    }

    private String description(CostSignal signal) {
        return switch (signal.signalType()) {
            case CACHE_HIT_RATIO -> "Cache hit ratio is below target. Review hot keys and raise safe TTLs.";
            case INDEX_STORAGE_GB -> "Search index storage exceeds target. Roll over index and review retention.";
            case HOT_STORAGE_GB -> "Hot storage exceeds target. Move eligible partitions to cold storage.";
            case COLD_STORAGE_GB -> "Cold storage exceeds target. Tighten data retention and archive policies.";
            case ASYNC_EXPORT_BACKLOG -> "Async export backlog exceeds target. Throttle exports or scale workers.";
            case HPA_REPLICA_WATERMARK -> "HPA replicas are close to max. Raise max replicas or add capacity.";
            case QPS_WATERMARK -> "QPS watermark is high. Add replicas or split traffic before the next peak.";
            case IDLE_RESOURCE -> "Resource utilization is low. Reduce idle replicas behind SLO protection.";
            case OVER_REPLICATED -> "Replica count exceeds demand. Right-size requests after SLO guard checks.";
        };
    }

    private CapacityRiskLevel riskLevel(int safeQps, int currentQps, int currentReplicas, int maxReplicas,
            BigDecimal cpuUtilization, BigDecimal memoryUtilization) {
        BigDecimal qpsWatermark =
                BigDecimal.valueOf(currentQps).divide(BigDecimal.valueOf(safeQps), 6, RoundingMode.HALF_UP);
        BigDecimal hpaWatermark =
                BigDecimal.valueOf(currentReplicas).divide(BigDecimal.valueOf(maxReplicas), 6, RoundingMode.HALF_UP);
        if (hpaWatermark.compareTo(new BigDecimal("0.85")) >= 0
                && qpsWatermark.compareTo(new BigDecimal("0.70")) >= 0) {
            return CapacityRiskLevel.HPA_NEAR_LIMIT;
        }
        if (qpsWatermark.compareTo(new BigDecimal("0.90")) >= 0) {
            return CapacityRiskLevel.SCALE_OUT_REQUIRED;
        }
        if (qpsWatermark.compareTo(new BigDecimal("0.20")) <= 0 && cpuUtilization.compareTo(new BigDecimal("0.30")) <= 0
                && memoryUtilization.compareTo(new BigDecimal("0.30")) <= 0 && currentReplicas > 2) {
            return CapacityRiskLevel.IDLE_RESOURCE;
        }
        if (currentReplicas >= recommendedReplicas(safeQps, currentQps, maxReplicas) * 2 && currentReplicas > 2) {
            return CapacityRiskLevel.OVER_REPLICATED;
        }
        return CapacityRiskLevel.NONE;
    }

    private int recommendedReplicas(int safeQps, int currentQps, int maxReplicas) {
        BigDecimal perReplicaSafeQps =
                BigDecimal.valueOf(safeQps).divide(BigDecimal.valueOf(maxReplicas), 6, RoundingMode.HALF_UP);
        BigDecimal recommended = BigDecimal.valueOf(currentQps).divide(perReplicaSafeQps, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("1.30"));
        return Math.max(2, recommended.setScale(0, RoundingMode.CEILING).intValue());
    }

    private String recommendation(CapacityRiskLevel riskLevel, boolean sloProtected) {
        return switch (riskLevel) {
            case HPA_NEAR_LIMIT -> "HPA is close to max replicas. Increase maxReplicas or add a new cell.";
            case SCALE_OUT_REQUIRED -> "Current QPS is above safe watermark. Scale out before accepting more traffic.";
            case IDLE_RESOURCE -> sloProtected
                    ? "Idle capacity detected. Reduce replicas gradually under SLO guard."
                    : "Idle capacity detected, but SLO guard is missing. Require manual review before reduction.";
            case OVER_REPLICATED -> sloProtected
                    ? "Replica count is higher than demand. Right-size requests safely."
                    : "Replica count is higher than demand, but SLO guard is missing. Keep capacity until guarded.";
            case NONE -> "Capacity is within the current safe operating envelope.";
        };
    }

    private CostSignalType capacitySignalType(CapacityRiskLevel riskLevel) {
        return switch (riskLevel) {
            case HPA_NEAR_LIMIT -> CostSignalType.HPA_REPLICA_WATERMARK;
            case SCALE_OUT_REQUIRED -> CostSignalType.QPS_WATERMARK;
            case IDLE_RESOURCE -> CostSignalType.IDLE_RESOURCE;
            case OVER_REPLICATED -> CostSignalType.OVER_REPLICATED;
            case NONE -> throw new IllegalArgumentException("capacity risk level has no signal");
        };
    }

    private CostActionType capacityActionType(CapacityRiskLevel riskLevel) {
        return switch (riskLevel) {
            case HPA_NEAR_LIMIT -> CostActionType.REVIEW_HPA_LIMIT;
            case SCALE_OUT_REQUIRED -> CostActionType.SCALE_SERVICE_REPLICAS;
            case IDLE_RESOURCE -> CostActionType.REDUCE_IDLE_REPLICAS;
            case OVER_REPLICATED -> CostActionType.RIGHTSIZE_RESOURCE_REQUESTS;
            case NONE -> throw new IllegalArgumentException("capacity risk level has no action");
        };
    }

    private int capacityPriority(CapacityRiskLevel riskLevel) {
        return switch (riskLevel) {
            case HPA_NEAR_LIMIT, SCALE_OUT_REQUIRED -> 1;
            case IDLE_RESOURCE, OVER_REPLICATED -> 2;
            case NONE -> 3;
        };
    }

    private void validateCapacityInputs(int safeQps, int peakQps, int currentQps, int currentReplicas,
            int maxReplicas) {
        if (safeQps <= 0 || peakQps <= 0 || currentQps < 0 || currentReplicas <= 0 || maxReplicas <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "capacity values are invalid");
        }
        if (currentReplicas > maxReplicas) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "current replicas cannot exceed max replicas");
        }
    }

    private String normalizeService(String serviceName) {
        String normalized = serviceName == null ? "" : serviceName.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "service name must not be blank");
        }
        return normalized;
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value.signum() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cost metric values must not be negative");
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal utilization(BigDecimal value, String name) {
        BigDecimal normalized = normalize(value);
        if (normalized.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, name + " must be between 0 and 1");
        }
        return normalized;
    }
}
