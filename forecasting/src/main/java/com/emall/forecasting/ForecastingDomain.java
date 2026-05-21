package com.emall.forecasting;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

enum ForecastRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

@TableName("demand_signal")
record DemandSignal(@TableId(value = "signal_id", type = IdType.INPUT) long signalId, long skuId, String regionCode,
        int soldQuantity, int pageViews, LocalDate signalDate, Instant createdAt) {
}

@TableName("demand_forecast")
record DemandForecast(@TableId(value = "forecast_id", type = IdType.INPUT) long forecastId, long skuId,
        String regionCode, int forecastQuantity, ForecastRiskLevel stockoutRisk, LocalDate forecastDate,
        Instant createdAt) {
}

@TableName("replenishment_plan")
record ReplenishmentPlan(@TableId(value = "plan_id", type = IdType.INPUT) long planId, long skuId,
        String warehouseCode, int recommendedQuantity, ForecastRiskLevel priority, LocalDate planDate,
        Instant createdAt) {
}

@TableName("capacity_forecast")
record CapacityForecast(@TableId(value = "capacity_forecast_id", type = IdType.INPUT) long capacityForecastId,
        String warehouseCode, int forecastOrders, int workerHours, ForecastRiskLevel pressureLevel,
        LocalDate forecastDate, Instant createdAt) {
}

record ForecastSummary(int demandSignals, int demandForecasts, int replenishmentPlans, int highRiskCount) {
}
