package com.emall.forecasting;

import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.localDateValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
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
        return forecastingMapper.findDemandSignals(skuId, regionCode).stream().map(this::mapDemandSignal).toList();
    }

    @Override
    public List<DemandSignal> findDemandSignals() {
        return forecastingMapper.findDemandSignals().stream().map(this::mapDemandSignal).toList();
    }

    @Override
    public DemandForecast saveDemandForecast(DemandForecast forecast) {
        forecastingMapper.saveDemandForecast(forecast);
        return forecast;
    }

    @Override
    public List<DemandForecast> findDemandForecasts() {
        return forecastingMapper.findDemandForecasts().stream().map(this::mapDemandForecast).toList();
    }

    @Override
    public ReplenishmentPlan saveReplenishmentPlan(ReplenishmentPlan plan) {
        forecastingMapper.saveReplenishmentPlan(plan);
        return plan;
    }

    @Override
    public List<ReplenishmentPlan> findReplenishmentPlans() {
        return forecastingMapper.findReplenishmentPlans().stream().map(this::mapReplenishmentPlan).toList();
    }

    @Override
    public CapacityForecast saveCapacityForecast(CapacityForecast forecast) {
        forecastingMapper.saveCapacityForecast(forecast);
        return forecast;
    }

    @Override
    public List<CapacityForecast> findCapacityForecasts() {
        return forecastingMapper.findCapacityForecasts().stream().map(this::mapCapacityForecast).toList();
    }

    private DemandSignal mapDemandSignal(Map<String, Object> row) {
        return new DemandSignal(longValue(row, "signal_id"), longValue(row, "sku_id"),
                stringValue(row, "region_code"), intValue(row, "sold_quantity"), intValue(row, "page_views"),
                localDateValue(row, "signal_date"), instantValue(row, "created_at"));
    }

    private DemandForecast mapDemandForecast(Map<String, Object> row) {
        return new DemandForecast(longValue(row, "forecast_id"), longValue(row, "sku_id"),
                stringValue(row, "region_code"), intValue(row, "forecast_quantity"),
                ForecastRiskLevel.valueOf(stringValue(row, "stockout_risk")), localDateValue(row, "forecast_date"),
                instantValue(row, "created_at"));
    }

    private ReplenishmentPlan mapReplenishmentPlan(Map<String, Object> row) {
        return new ReplenishmentPlan(longValue(row, "plan_id"), longValue(row, "sku_id"),
                stringValue(row, "warehouse_code"), intValue(row, "recommended_quantity"),
                ForecastRiskLevel.valueOf(stringValue(row, "priority")), localDateValue(row, "plan_date"),
                instantValue(row, "created_at"));
    }

    private CapacityForecast mapCapacityForecast(Map<String, Object> row) {
        return new CapacityForecast(longValue(row, "capacity_forecast_id"), stringValue(row, "warehouse_code"),
                intValue(row, "forecast_orders"), intValue(row, "worker_hours"),
                ForecastRiskLevel.valueOf(stringValue(row, "pressure_level")), localDateValue(row, "forecast_date"),
                instantValue(row, "created_at"));
    }
}
