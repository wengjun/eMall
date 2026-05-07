package com.emall.cost.repository;

import com.emall.cost.domain.CostActionStatus;
import com.emall.cost.domain.CostActionType;
import com.emall.cost.domain.CostBudget;
import com.emall.cost.domain.CostOptimizationAction;
import com.emall.cost.domain.CostSignal;
import com.emall.cost.domain.CostSignalType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class JdbcCostRepository implements CostRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CostSignal saveSignal(CostSignal signal) {
        jdbcTemplate.update("""
                INSERT INTO cost_signal
                    (signal_id, service_name, signal_type, metric_value, threshold_value, observed_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, signal.signalId(), signal.serviceName(), signal.signalType().name(), signal.metricValue(),
                signal.thresholdValue(), Timestamp.from(signal.observedAt()), Timestamp.from(signal.createdAt()));
        return signal;
    }

    @Override
    public List<CostSignal> findSignalsByService(String serviceName, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM cost_signal
                WHERE service_name = ?
                ORDER BY observed_at DESC
                LIMIT ?
                """, this::mapSignal, serviceName, limit);
    }

    @Override
    public CostBudget saveBudget(CostBudget budget) {
        jdbcTemplate.update("""
                INSERT INTO cost_budget
                    (budget_id, service_name, monthly_budget, current_spend, currency, alert_threshold_percent,
                    active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE monthly_budget = VALUES(monthly_budget),
                    current_spend = VALUES(current_spend), currency = VALUES(currency),
                    alert_threshold_percent = VALUES(alert_threshold_percent), active = VALUES(active),
                    updated_at = VALUES(updated_at)
                """, budget.budgetId(), budget.serviceName(), budget.monthlyBudget(), budget.currentSpend(),
                budget.currency(), budget.alertThresholdPercent(), budget.active(), Timestamp.from(budget.createdAt()),
                Timestamp.from(budget.updatedAt()));
        return budget;
    }

    @Override
    public Optional<CostBudget> findActiveBudget(String serviceName) {
        return jdbcTemplate.query("""
                SELECT * FROM cost_budget
                WHERE service_name = ? AND active = TRUE
                LIMIT 1
                """, this::mapBudget, serviceName).stream().findFirst();
    }

    @Override
    public CostOptimizationAction saveAction(CostOptimizationAction action) {
        jdbcTemplate.update("""
                INSERT INTO cost_optimization_action
                    (action_id, service_name, signal_type, action_type, status, priority, description, created_at,
                    updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), priority = VALUES(priority),
                    description = VALUES(description), updated_at = VALUES(updated_at)
                """, action.actionId(), action.serviceName(), action.signalType().name(), action.actionType().name(),
                action.status().name(), action.priority(), action.description(), Timestamp.from(action.createdAt()),
                Timestamp.from(action.updatedAt()));
        return action;
    }

    @Override
    public Optional<CostOptimizationAction> findAction(long actionId) {
        return jdbcTemplate
                .query("SELECT * FROM cost_optimization_action WHERE action_id = ?", this::mapAction, actionId).stream()
                .findFirst();
    }

    @Override
    public Optional<CostOptimizationAction> findActiveAction(String serviceName, CostSignalType signalType,
            CostActionType actionType) {
        return jdbcTemplate.query("""
                SELECT * FROM cost_optimization_action
                WHERE service_name = ? AND signal_type = ? AND action_type = ?
                    AND status IN ('OPEN', 'ACKNOWLEDGED')
                ORDER BY updated_at DESC
                LIMIT 1
                """, this::mapAction, serviceName, signalType.name(), actionType.name()).stream().findFirst();
    }

    @Override
    public List<CostOptimizationAction> findActiveActionsByService(String serviceName) {
        return jdbcTemplate.query("""
                SELECT * FROM cost_optimization_action
                WHERE service_name = ? AND status IN ('OPEN', 'ACKNOWLEDGED')
                ORDER BY priority ASC, updated_at DESC
                """, this::mapAction, serviceName);
    }

    private CostSignal mapSignal(ResultSet rs, int rowNum) throws SQLException {
        return new CostSignal(rs.getLong("signal_id"), rs.getString("service_name"),
                CostSignalType.valueOf(rs.getString("signal_type")), rs.getBigDecimal("metric_value"),
                rs.getBigDecimal("threshold_value"), rs.getTimestamp("observed_at").toInstant(),
                rs.getTimestamp("created_at").toInstant());
    }

    private CostBudget mapBudget(ResultSet rs, int rowNum) throws SQLException {
        return new CostBudget(rs.getLong("budget_id"), rs.getString("service_name"), rs.getBigDecimal("monthly_budget"),
                rs.getBigDecimal("current_spend"), rs.getString("currency"), rs.getInt("alert_threshold_percent"),
                rs.getBoolean("active"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private CostOptimizationAction mapAction(ResultSet rs, int rowNum) throws SQLException {
        return new CostOptimizationAction(rs.getLong("action_id"), rs.getString("service_name"),
                CostSignalType.valueOf(rs.getString("signal_type")),
                CostActionType.valueOf(rs.getString("action_type")), CostActionStatus.valueOf(rs.getString("status")),
                rs.getInt("priority"), rs.getString("description"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
