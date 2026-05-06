package com.emall.forecasting;

import java.util.List;

interface ForecastingRepository {
    DemandSignal saveDemandSignal(DemandSignal signal);

    List<DemandSignal> findDemandSignals(long skuId, String regionCode);

    List<DemandSignal> findDemandSignals();

    DemandForecast saveDemandForecast(DemandForecast forecast);

    List<DemandForecast> findDemandForecasts();

    ReplenishmentPlan saveReplenishmentPlan(ReplenishmentPlan plan);

    List<ReplenishmentPlan> findReplenishmentPlans();

    CapacityForecast saveCapacityForecast(CapacityForecast forecast);

    List<CapacityForecast> findCapacityForecasts();
}
