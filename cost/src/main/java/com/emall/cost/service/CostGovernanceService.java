package com.emall.cost.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.cost.domain.CostActionStatus;
import com.emall.cost.domain.CostActionType;
import com.emall.cost.domain.CostBudget;
import com.emall.cost.domain.CostOptimizationAction;
import com.emall.cost.domain.CostSignal;
import com.emall.cost.domain.CostSignalType;
import com.emall.cost.domain.CostSummary;
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
        CostBudget budget = repository.findActiveBudget(normalizedService)
                .map(existing -> existing.update(normalize(monthlyBudget), normalize(currentSpend),
                        normalizedCurrency, alertThresholdPercent, active))
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    return new CostBudget(idGenerator.nextId(), normalizedService, normalize(monthlyBudget),
                            normalize(currentSpend), normalizedCurrency, alertThresholdPercent, active, now, now);
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
        CostBudget budget = repository.findActiveBudget(normalizedService)
                .orElse(new CostBudget(0L, normalizedService, BigDecimal.ZERO, BigDecimal.ZERO, "USD", 100,
                        false, Instant.EPOCH, Instant.EPOCH));
        return new CostSummary(normalizedService, budget.monthlyBudget(), budget.currentSpend(), budget.currency(),
                budget.alertThresholdPercent(), budget.active() && budget.overAlertThreshold(),
                repository.findSignalsByService(normalizedService, 50).size(),
                repository.findActiveActionsByService(normalizedService).size());
    }

    private void openAction(CostSignal signal) {
        CostActionType actionType = actionType(signal.signalType());
        repository.findActiveAction(signal.serviceName(), signal.signalType(), actionType)
                .orElseGet(() -> repository.saveAction(new CostOptimizationAction(idGenerator.nextId(),
                        signal.serviceName(), signal.signalType(), actionType, CostActionStatus.OPEN,
                        priority(signal.signalType()), description(signal), Instant.now(), Instant.now())));
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
        };
    }

    private int priority(CostSignalType signalType) {
        return switch (signalType) {
            case CACHE_HIT_RATIO, ASYNC_EXPORT_BACKLOG -> 1;
            case HOT_STORAGE_GB, INDEX_STORAGE_GB -> 2;
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
        };
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
}
