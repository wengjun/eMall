package com.emall.cost.repository;

import com.emall.cost.domain.CostActionStatus;
import com.emall.cost.domain.CostActionType;
import com.emall.cost.domain.CostBudget;
import com.emall.cost.domain.CostOptimizationAction;
import com.emall.cost.domain.CostSignal;
import com.emall.cost.domain.CostSignalType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryCostRepository implements CostRepository {
    private final ConcurrentMap<Long, CostSignal> signals = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CostBudget> budgets = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, CostOptimizationAction> actions = new ConcurrentHashMap<>();

    @Override
    public CostSignal saveSignal(CostSignal signal) {
        signals.put(signal.signalId(), signal);
        return signal;
    }

    @Override
    public List<CostSignal> findSignalsByService(String serviceName, int limit) {
        return signals.values().stream()
                .filter(signal -> signal.serviceName().equals(serviceName))
                .sorted(Comparator.comparing(CostSignal::observedAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public CostBudget saveBudget(CostBudget budget) {
        budgets.put(budget.serviceName(), budget);
        return budget;
    }

    @Override
    public Optional<CostBudget> findActiveBudget(String serviceName) {
        return Optional.ofNullable(budgets.get(serviceName)).filter(CostBudget::active);
    }

    @Override
    public CostOptimizationAction saveAction(CostOptimizationAction action) {
        actions.put(action.actionId(), action);
        return action;
    }

    @Override
    public Optional<CostOptimizationAction> findAction(long actionId) {
        return Optional.ofNullable(actions.get(actionId));
    }

    @Override
    public Optional<CostOptimizationAction> findActiveAction(String serviceName, CostSignalType signalType,
                                                            CostActionType actionType) {
        return actions.values().stream()
                .filter(action -> action.serviceName().equals(serviceName))
                .filter(action -> action.signalType() == signalType)
                .filter(action -> action.actionType() == actionType)
                .filter(this::isActive)
                .max(Comparator.comparing(CostOptimizationAction::updatedAt));
    }

    @Override
    public List<CostOptimizationAction> findActiveActionsByService(String serviceName) {
        return actions.values().stream()
                .filter(action -> action.serviceName().equals(serviceName))
                .filter(this::isActive)
                .sorted(Comparator.comparingInt(CostOptimizationAction::priority)
                        .thenComparing(Comparator.comparing(CostOptimizationAction::updatedAt).reversed()))
                .toList();
    }

    private boolean isActive(CostOptimizationAction action) {
        return action.status() == CostActionStatus.OPEN || action.status() == CostActionStatus.ACKNOWLEDGED;
    }
}
