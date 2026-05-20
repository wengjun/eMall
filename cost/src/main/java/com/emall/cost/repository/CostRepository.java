package com.emall.cost.repository;

import com.emall.cost.domain.CostActionType;
import com.emall.cost.domain.CostBudget;
import com.emall.cost.domain.CostOptimizationAction;
import com.emall.cost.domain.CostSignal;
import com.emall.cost.domain.CostSignalType;
import com.emall.cost.domain.ServiceCapacityBaseline;
import java.util.List;
import java.util.Optional;

public interface CostRepository {
    CostSignal saveSignal(CostSignal signal);

    List<CostSignal> findSignalsByService(String serviceName, int limit);

    CostBudget saveBudget(CostBudget budget);

    Optional<CostBudget> findActiveBudget(String serviceName);

    CostOptimizationAction saveAction(CostOptimizationAction action);

    Optional<CostOptimizationAction> findAction(long actionId);

    Optional<CostOptimizationAction> findActiveAction(String serviceName, CostSignalType signalType,
            CostActionType actionType);

    List<CostOptimizationAction> findActiveActionsByService(String serviceName);

    ServiceCapacityBaseline saveCapacityBaseline(ServiceCapacityBaseline baseline);

    Optional<ServiceCapacityBaseline> findLatestCapacityBaseline(String serviceName);
}
