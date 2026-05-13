package com.emall.forecasting;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface ForecastingMapper {
    @Insert("""
            INSERT INTO demand_signal
                (signal_id, sku_id, region_code, sold_quantity, page_views, signal_date, created_at)
            VALUES (#{signal.signalId}, #{signal.skuId}, #{signal.regionCode}, #{signal.soldQuantity},
                #{signal.pageViews}, #{signal.signalDate}, #{signal.createdAt})
            """)
    int saveDemandSignal(@Param("signal") DemandSignal signal);

    @Select("""
            SELECT signal_id, sku_id, region_code, sold_quantity, page_views, signal_date, created_at
            FROM demand_signal
            WHERE sku_id = #{skuId} AND region_code = #{regionCode}
            """)
    List<DemandSignal> findDemandSignals(@Param("skuId") long skuId, @Param("regionCode") String regionCode);

    @Select("""
            SELECT signal_id, sku_id, region_code, sold_quantity, page_views, signal_date, created_at
            FROM demand_signal
            """)
    List<DemandSignal> findDemandSignals();

    @Insert("""
            INSERT INTO demand_forecast
                (forecast_id, sku_id, region_code, forecast_quantity, stockout_risk, forecast_date, created_at)
            VALUES (#{forecast.forecastId}, #{forecast.skuId}, #{forecast.regionCode},
                #{forecast.forecastQuantity}, #{forecast.stockoutRisk}, #{forecast.forecastDate},
                #{forecast.createdAt})
            """)
    int saveDemandForecast(@Param("forecast") DemandForecast forecast);

    @Select("""
            SELECT forecast_id, sku_id, region_code, forecast_quantity, stockout_risk, forecast_date, created_at
            FROM demand_forecast
            """)
    List<DemandForecast> findDemandForecasts();

    @Insert("""
            INSERT INTO replenishment_plan
                (plan_id, sku_id, warehouse_code, recommended_quantity, priority, plan_date, created_at)
            VALUES (#{plan.planId}, #{plan.skuId}, #{plan.warehouseCode}, #{plan.recommendedQuantity},
                #{plan.priority}, #{plan.planDate}, #{plan.createdAt})
            """)
    int saveReplenishmentPlan(@Param("plan") ReplenishmentPlan plan);

    @Select("""
            SELECT plan_id, sku_id, warehouse_code, recommended_quantity, priority, plan_date, created_at
            FROM replenishment_plan
            """)
    List<ReplenishmentPlan> findReplenishmentPlans();

    @Insert("""
            INSERT INTO capacity_forecast
                (capacity_forecast_id, warehouse_code, forecast_orders, worker_hours, pressure_level,
                forecast_date, created_at)
            VALUES (#{forecast.capacityForecastId}, #{forecast.warehouseCode}, #{forecast.forecastOrders},
                #{forecast.workerHours}, #{forecast.pressureLevel}, #{forecast.forecastDate},
                #{forecast.createdAt})
            """)
    int saveCapacityForecast(@Param("forecast") CapacityForecast forecast);

    @Select("""
            SELECT capacity_forecast_id, warehouse_code, forecast_orders, worker_hours, pressure_level,
                forecast_date, created_at
            FROM capacity_forecast
            """)
    List<CapacityForecast> findCapacityForecasts();
}
