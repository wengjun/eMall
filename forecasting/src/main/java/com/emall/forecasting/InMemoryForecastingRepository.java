package com.emall.forecasting;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryForecastingRepository implements ForecastingRepository {
    private final ConcurrentMap<Long, DemandSignal> signals = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, DemandForecast> forecasts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ReplenishmentPlan> plans = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, CapacityForecast> capacityForecasts = new ConcurrentHashMap<>();

    @Override
    public DemandSignal saveDemandSignal(DemandSignal signal) {
        signals.put(signal.signalId(), signal);
        return signal;
    }

    @Override
    public List<DemandSignal> findDemandSignals(long skuId, String regionCode) {
        return signals.values().stream()
                .filter(signal -> signal.skuId() == skuId && signal.regionCode().equals(regionCode)).toList();
    }

    @Override
    public List<DemandSignal> findDemandSignals() {
        return List.copyOf(signals.values());
    }

    @Override
    public DemandForecast saveDemandForecast(DemandForecast forecast) {
        forecasts.put(forecast.forecastId(), forecast);
        return forecast;
    }

    @Override
    public List<DemandForecast> findDemandForecasts() {
        return List.copyOf(forecasts.values());
    }

    @Override
    public ReplenishmentPlan saveReplenishmentPlan(ReplenishmentPlan plan) {
        plans.put(plan.planId(), plan);
        return plan;
    }

    @Override
    public List<ReplenishmentPlan> findReplenishmentPlans() {
        return List.copyOf(plans.values());
    }

    @Override
    public CapacityForecast saveCapacityForecast(CapacityForecast forecast) {
        capacityForecasts.put(forecast.capacityForecastId(), forecast);
        return forecast;
    }

    @Override
    public List<CapacityForecast> findCapacityForecasts() {
        return List.copyOf(capacityForecasts.values());
    }
}
