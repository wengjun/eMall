package com.emall.forecasting;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcForecastingRepository implements ForecastingRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcForecastingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DemandSignal saveDemandSignal(DemandSignal signal) {
        jdbcTemplate.update("""
                INSERT INTO demand_signal
                    (signal_id, sku_id, region_code, sold_quantity, page_views, signal_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, signal.signalId(), signal.skuId(), signal.regionCode(), signal.soldQuantity(), signal.pageViews(),
                Date.valueOf(signal.signalDate()), Timestamp.from(signal.createdAt()));
        return signal;
    }

    @Override
    public List<DemandSignal> findDemandSignals(long skuId, String regionCode) {
        return jdbcTemplate.query("""
                SELECT * FROM demand_signal
                WHERE sku_id = ? AND region_code = ?
                """, this::mapDemandSignal, skuId, regionCode);
    }

    @Override
    public List<DemandSignal> findDemandSignals() {
        return jdbcTemplate.query("SELECT * FROM demand_signal", this::mapDemandSignal);
    }

    @Override
    public DemandForecast saveDemandForecast(DemandForecast forecast) {
        jdbcTemplate.update("""
                INSERT INTO demand_forecast
                    (forecast_id, sku_id, region_code, forecast_quantity, stockout_risk, forecast_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, forecast.forecastId(), forecast.skuId(), forecast.regionCode(), forecast.forecastQuantity(),
                forecast.stockoutRisk().name(), Date.valueOf(forecast.forecastDate()),
                Timestamp.from(forecast.createdAt()));
        return forecast;
    }

    @Override
    public List<DemandForecast> findDemandForecasts() {
        return jdbcTemplate.query("SELECT * FROM demand_forecast", this::mapDemandForecast);
    }

    @Override
    public ReplenishmentPlan saveReplenishmentPlan(ReplenishmentPlan plan) {
        jdbcTemplate.update("""
                INSERT INTO replenishment_plan
                    (plan_id, sku_id, warehouse_code, recommended_quantity, priority, plan_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, plan.planId(), plan.skuId(), plan.warehouseCode(), plan.recommendedQuantity(),
                plan.priority().name(), Date.valueOf(plan.planDate()), Timestamp.from(plan.createdAt()));
        return plan;
    }

    @Override
    public List<ReplenishmentPlan> findReplenishmentPlans() {
        return jdbcTemplate.query("SELECT * FROM replenishment_plan", this::mapReplenishmentPlan);
    }

    @Override
    public CapacityForecast saveCapacityForecast(CapacityForecast forecast) {
        jdbcTemplate.update("""
                INSERT INTO capacity_forecast
                    (capacity_forecast_id, warehouse_code, forecast_orders, worker_hours, pressure_level,
                    forecast_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, forecast.capacityForecastId(), forecast.warehouseCode(), forecast.forecastOrders(),
                forecast.workerHours(), forecast.pressureLevel().name(), Date.valueOf(forecast.forecastDate()),
                Timestamp.from(forecast.createdAt()));
        return forecast;
    }

    @Override
    public List<CapacityForecast> findCapacityForecasts() {
        return jdbcTemplate.query("SELECT * FROM capacity_forecast", this::mapCapacityForecast);
    }

    private DemandSignal mapDemandSignal(ResultSet rs, int rowNum) throws SQLException {
        return new DemandSignal(rs.getLong("signal_id"), rs.getLong("sku_id"), rs.getString("region_code"),
                rs.getInt("sold_quantity"), rs.getInt("page_views"), rs.getDate("signal_date").toLocalDate(),
                rs.getTimestamp("created_at").toInstant());
    }

    private DemandForecast mapDemandForecast(ResultSet rs, int rowNum) throws SQLException {
        return new DemandForecast(rs.getLong("forecast_id"), rs.getLong("sku_id"), rs.getString("region_code"),
                rs.getInt("forecast_quantity"), ForecastRiskLevel.valueOf(rs.getString("stockout_risk")),
                rs.getDate("forecast_date").toLocalDate(), rs.getTimestamp("created_at").toInstant());
    }

    private ReplenishmentPlan mapReplenishmentPlan(ResultSet rs, int rowNum) throws SQLException {
        return new ReplenishmentPlan(rs.getLong("plan_id"), rs.getLong("sku_id"), rs.getString("warehouse_code"),
                rs.getInt("recommended_quantity"), ForecastRiskLevel.valueOf(rs.getString("priority")),
                rs.getDate("plan_date").toLocalDate(), rs.getTimestamp("created_at").toInstant());
    }

    private CapacityForecast mapCapacityForecast(ResultSet rs, int rowNum) throws SQLException {
        return new CapacityForecast(rs.getLong("capacity_forecast_id"), rs.getString("warehouse_code"),
                rs.getInt("forecast_orders"), rs.getInt("worker_hours"),
                ForecastRiskLevel.valueOf(rs.getString("pressure_level")), rs.getDate("forecast_date").toLocalDate(),
                rs.getTimestamp("created_at").toInstant());
    }
}
