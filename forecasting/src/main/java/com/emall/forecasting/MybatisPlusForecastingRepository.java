package com.emall.forecasting;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusForecastingRepository implements ForecastingRepository {
    private final ForecastingMapper forecastingMapper;

    MybatisPlusForecastingRepository(ForecastingMapper forecastingMapper) {
        this.forecastingMapper = forecastingMapper;
    }

    @Override
    public DemandSignal saveDemandSignal(DemandSignal signal) {
        forecastingMapper.saveDemandSignal(signal);
        return signal;
    }

    @Override
    public List<DemandSignal> findDemandSignals(long skuId, String regionCode) {
        return forecastingMapper.findDemandSignals(skuId, regionCode);
    }

    @Override
    public List<DemandSignal> findDemandSignals() {
        return forecastingMapper.findDemandSignals();
    }

    @Override
    public DemandForecast saveDemandForecast(DemandForecast forecast) {
        forecastingMapper.saveDemandForecast(forecast);
        return forecast;
    }

    @Override
    public List<DemandForecast> findDemandForecasts() {
        return forecastingMapper.findDemandForecasts();
    }

    @Override
    public ReplenishmentPlan saveReplenishmentPlan(ReplenishmentPlan plan) {
        forecastingMapper.saveReplenishmentPlan(plan);
        return plan;
    }

    @Override
    public List<ReplenishmentPlan> findReplenishmentPlans() {
        return forecastingMapper.findReplenishmentPlans();
    }

    @Override
    public CapacityForecast saveCapacityForecast(CapacityForecast forecast) {
        forecastingMapper.saveCapacityForecast(forecast);
        return forecast;
    }

    @Override
    public List<CapacityForecast> findCapacityForecasts() {
        return forecastingMapper.findCapacityForecasts();
    }
}
