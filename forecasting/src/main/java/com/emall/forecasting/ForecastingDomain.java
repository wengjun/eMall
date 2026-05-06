package com.emall.forecasting;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

enum ForecastRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

record DemandSignal(long signalId, long skuId, String regionCode, int soldQuantity, int pageViews,
                    LocalDate signalDate, Instant createdAt) {
}

record DemandForecast(long forecastId, long skuId, String regionCode, int forecastQuantity,
                      ForecastRiskLevel stockoutRisk, LocalDate forecastDate, Instant createdAt) {
}

record ReplenishmentPlan(long planId, long skuId, String warehouseCode, int recommendedQuantity,
                         ForecastRiskLevel priority, LocalDate planDate, Instant createdAt) {
}

record CapacityForecast(long capacityForecastId, String warehouseCode, int forecastOrders, int workerHours,
                        ForecastRiskLevel pressureLevel, LocalDate forecastDate, Instant createdAt) {
}

record ForecastSummary(int demandSignals, int demandForecasts, int replenishmentPlans, int highRiskCount) {
}
