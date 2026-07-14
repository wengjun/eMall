package com.emall.forecasting;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.common.persistence.BoundedQuery;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusForecastingRepository implements ForecastingRepository {
    private final DemandSignalMapper demandSignalMapper;
    private final DemandForecastMapper demandForecastMapper;
    private final ReplenishmentPlanMapper replenishmentPlanMapper;
    private final CapacityForecastMapper capacityForecastMapper;

    MybatisPlusForecastingRepository(DemandSignalMapper demandSignalMapper, DemandForecastMapper demandForecastMapper,
            ReplenishmentPlanMapper replenishmentPlanMapper, CapacityForecastMapper capacityForecastMapper) {
        this.demandSignalMapper = demandSignalMapper;
        this.demandForecastMapper = demandForecastMapper;
        this.replenishmentPlanMapper = replenishmentPlanMapper;
        this.capacityForecastMapper = capacityForecastMapper;
    }

    @Override
    public DemandSignal saveDemandSignal(DemandSignal signal) {
        demandSignalMapper.insert(signal);
        return signal;
    }

    @Override
    public List<DemandSignal> findDemandSignals(long skuId, String regionCode) {
        return BoundedQuery.firstPage(demandSignalMapper,
                new QueryWrapper<DemandSignal>().eq("sku_id", skuId).eq("region_code", regionCode));
    }

    @Override
    public List<DemandSignal> findDemandSignals() {
        return BoundedQuery.firstPage(demandSignalMapper);
    }

    @Override
    public DemandForecast saveDemandForecast(DemandForecast forecast) {
        demandForecastMapper.insert(forecast);
        return forecast;
    }

    @Override
    public List<DemandForecast> findDemandForecasts() {
        return BoundedQuery.firstPage(demandForecastMapper);
    }

    @Override
    public ReplenishmentPlan saveReplenishmentPlan(ReplenishmentPlan plan) {
        replenishmentPlanMapper.insert(plan);
        return plan;
    }

    @Override
    public List<ReplenishmentPlan> findReplenishmentPlans() {
        return BoundedQuery.firstPage(replenishmentPlanMapper);
    }

    @Override
    public CapacityForecast saveCapacityForecast(CapacityForecast forecast) {
        capacityForecastMapper.insert(forecast);
        return forecast;
    }

    @Override
    public List<CapacityForecast> findCapacityForecasts() {
        return BoundedQuery.firstPage(capacityForecastMapper);
    }
}
