package com.emall.cost.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.cost.domain.CostActionStatus;
import com.emall.cost.domain.CostActionType;
import com.emall.cost.domain.CostBudget;
import com.emall.cost.domain.CostOptimizationAction;
import com.emall.cost.domain.CostSignal;
import com.emall.cost.domain.CostSignalType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusCostRepository implements CostRepository {
    private final CostSignalMapper signalMapper;
    private final CostBudgetMapper budgetMapper;
    private final CostOptimizationActionMapper actionMapper;

    public MybatisPlusCostRepository(CostSignalMapper signalMapper, CostBudgetMapper budgetMapper,
            CostOptimizationActionMapper actionMapper) {
        this.signalMapper = signalMapper;
        this.budgetMapper = budgetMapper;
        this.actionMapper = actionMapper;
    }

    @Override
    public CostSignal saveSignal(CostSignal signal) {
        signalMapper.insert(toEntity(signal));
        return signal;
    }

    @Override
    public List<CostSignal> findSignalsByService(String serviceName, int limit) {
        return signalMapper.selectList(new QueryWrapper<CostSignalEntity>()
                .eq("service_name", serviceName)
                .orderByDesc("observed_at")
                .last("LIMIT " + limit)).stream().map(this::toDomain).toList();
    }

    @Override
    public CostBudget saveBudget(CostBudget budget) {
        CostBudgetEntity entity = toEntity(budget);
        try {
            budgetMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            budgetMapper.update(null, new UpdateWrapper<CostBudgetEntity>()
                    .set("monthly_budget", entity.getMonthlyBudget())
                    .set("current_spend", entity.getCurrentSpend())
                    .set("currency", entity.getCurrency())
                    .set("alert_threshold_percent", entity.getAlertThresholdPercent())
                    .set("active", entity.getActive())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("budget_id", entity.getBudgetId()));
        }
        return budget;
    }

    @Override
    public Optional<CostBudget> findActiveBudget(String serviceName) {
        return Optional.ofNullable(budgetMapper.selectOne(new QueryWrapper<CostBudgetEntity>()
                .eq("service_name", serviceName)
                .eq("active", true)
                .last("LIMIT 1"))).map(this::toDomain);
    }

    @Override
    public CostOptimizationAction saveAction(CostOptimizationAction action) {
        CostOptimizationActionEntity entity = toEntity(action);
        try {
            actionMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            actionMapper.update(null, new UpdateWrapper<CostOptimizationActionEntity>()
                    .set("status", entity.getStatus())
                    .set("priority", entity.getPriority())
                    .set("description", entity.getDescription())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("action_id", entity.getActionId()));
        }
        return action;
    }

    @Override
    public Optional<CostOptimizationAction> findAction(long actionId) {
        return Optional.ofNullable(actionMapper.selectById(actionId)).map(this::toDomain);
    }

    @Override
    public Optional<CostOptimizationAction> findActiveAction(String serviceName, CostSignalType signalType,
            CostActionType actionType) {
        return Optional.ofNullable(actionMapper.selectOne(new QueryWrapper<CostOptimizationActionEntity>()
                .eq("service_name", serviceName)
                .eq("signal_type", signalType.name())
                .eq("action_type", actionType.name())
                .in("status", CostActionStatus.OPEN.name(), CostActionStatus.ACKNOWLEDGED.name())
                .orderByDesc("updated_at")
                .last("LIMIT 1"))).map(this::toDomain);
    }

    @Override
    public List<CostOptimizationAction> findActiveActionsByService(String serviceName) {
        return actionMapper.selectList(new QueryWrapper<CostOptimizationActionEntity>()
                .eq("service_name", serviceName)
                .in("status", CostActionStatus.OPEN.name(), CostActionStatus.ACKNOWLEDGED.name())
                .orderByAsc("priority")
                .orderByDesc("updated_at")).stream().map(this::toDomain).toList();
    }

    private CostSignalEntity toEntity(CostSignal signal) {
        CostSignalEntity entity = new CostSignalEntity();
        entity.setSignalId(signal.signalId());
        entity.setServiceName(signal.serviceName());
        entity.setSignalType(signal.signalType().name());
        entity.setMetricValue(signal.metricValue());
        entity.setThresholdValue(signal.thresholdValue());
        entity.setObservedAt(LocalDateTime.ofInstant(signal.observedAt(), ZoneOffset.UTC));
        entity.setCreatedAt(LocalDateTime.ofInstant(signal.createdAt(), ZoneOffset.UTC));
        return entity;
    }

    private CostSignal toDomain(CostSignalEntity entity) {
        return new CostSignal(entity.getSignalId(), entity.getServiceName(),
                CostSignalType.valueOf(entity.getSignalType()), entity.getMetricValue(), entity.getThresholdValue(),
                entity.getObservedAt().toInstant(ZoneOffset.UTC), entity.getCreatedAt().toInstant(ZoneOffset.UTC));
    }

    private CostBudgetEntity toEntity(CostBudget budget) {
        CostBudgetEntity entity = new CostBudgetEntity();
        entity.setBudgetId(budget.budgetId());
        entity.setServiceName(budget.serviceName());
        entity.setMonthlyBudget(budget.monthlyBudget());
        entity.setCurrentSpend(budget.currentSpend());
        entity.setCurrency(budget.currency());
        entity.setAlertThresholdPercent(budget.alertThresholdPercent());
        entity.setActive(budget.active());
        entity.setCreatedAt(LocalDateTime.ofInstant(budget.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(budget.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private CostBudget toDomain(CostBudgetEntity entity) {
        return new CostBudget(entity.getBudgetId(), entity.getServiceName(), entity.getMonthlyBudget(),
                entity.getCurrentSpend(), entity.getCurrency(), entity.getAlertThresholdPercent(), entity.getActive(),
                entity.getCreatedAt().toInstant(ZoneOffset.UTC), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private CostOptimizationActionEntity toEntity(CostOptimizationAction action) {
        CostOptimizationActionEntity entity = new CostOptimizationActionEntity();
        entity.setActionId(action.actionId());
        entity.setServiceName(action.serviceName());
        entity.setSignalType(action.signalType().name());
        entity.setActionType(action.actionType().name());
        entity.setStatus(action.status().name());
        entity.setPriority(action.priority());
        entity.setDescription(action.description());
        entity.setCreatedAt(LocalDateTime.ofInstant(action.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(action.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private CostOptimizationAction toDomain(CostOptimizationActionEntity entity) {
        return new CostOptimizationAction(entity.getActionId(), entity.getServiceName(),
                CostSignalType.valueOf(entity.getSignalType()), CostActionType.valueOf(entity.getActionType()),
                CostActionStatus.valueOf(entity.getStatus()), entity.getPriority(), entity.getDescription(),
                entity.getCreatedAt().toInstant(ZoneOffset.UTC), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
