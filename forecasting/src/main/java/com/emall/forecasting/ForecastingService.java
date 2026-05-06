package com.emall.forecasting;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ForecastingService {
    private final ForecastingRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    ForecastingService(ForecastingRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    DemandSignal recordDemandSignal(long skuId, String regionCode, int soldQuantity, int pageViews,
                                    LocalDate signalDate) {
        requireNonNegative(soldQuantity, "sold quantity must not be negative");
        requireNonNegative(pageViews, "page views must not be negative");
        return repository.saveDemandSignal(new DemandSignal(idGenerator.nextId(), skuId, normalize(regionCode),
                soldQuantity, pageViews, signalDate, Instant.now()));
    }

    @Transactional
    DemandForecast buildDemandForecast(long skuId, String regionCode, int currentStock, LocalDate forecastDate) {
        List<DemandSignal> signals = repository.findDemandSignals(skuId, normalize(regionCode));
        int averageDemand = signals.isEmpty()
                ? 0
                : signals.stream().mapToInt(DemandSignal::soldQuantity).sum() / signals.size();
        int forecastQuantity = Math.max(averageDemand, averageDemand + averageDemand / 5);
        ForecastRiskLevel risk = riskByCoverage(currentStock, forecastQuantity);
        DemandForecast forecast = new DemandForecast(idGenerator.nextId(), skuId, normalize(regionCode),
                forecastQuantity, risk, forecastDate, Instant.now());
        return repository.saveDemandForecast(forecast);
    }

    @Transactional
    ReplenishmentPlan createReplenishmentPlan(long skuId, String warehouseCode, int forecastQuantity,
                                              int availableStock, LocalDate planDate) {
        int gap = Math.max(0, forecastQuantity - availableStock);
        ForecastRiskLevel priority = gap > forecastQuantity / 2 ? ForecastRiskLevel.HIGH
                : gap > 0 ? ForecastRiskLevel.MEDIUM : ForecastRiskLevel.LOW;
        return repository.saveReplenishmentPlan(new ReplenishmentPlan(idGenerator.nextId(), skuId,
                normalize(warehouseCode), gap, priority, planDate, Instant.now()));
    }

    @Transactional
    CapacityForecast createCapacityForecast(String warehouseCode, int forecastOrders, int workerHours,
                                            LocalDate forecastDate) {
        requireNonNegative(forecastOrders, "forecast orders must not be negative");
        requireNonNegative(workerHours, "worker hours must not be negative");
        int ordersPerHour = workerHours == 0 ? forecastOrders : forecastOrders / workerHours;
        ForecastRiskLevel pressure = ordersPerHour > 25 ? ForecastRiskLevel.HIGH
                : ordersPerHour > 15 ? ForecastRiskLevel.MEDIUM : ForecastRiskLevel.LOW;
        return repository.saveCapacityForecast(new CapacityForecast(idGenerator.nextId(), normalize(warehouseCode),
                forecastOrders, workerHours, pressure, forecastDate, Instant.now()));
    }

    ForecastSummary summary() {
        int highRisk = (int) repository.findDemandForecasts().stream()
                .filter(forecast -> forecast.stockoutRisk() == ForecastRiskLevel.HIGH)
                .count();
        highRisk += (int) repository.findReplenishmentPlans().stream()
                .filter(plan -> plan.priority() == ForecastRiskLevel.HIGH)
                .count();
        highRisk += (int) repository.findCapacityForecasts().stream()
                .filter(forecast -> forecast.pressureLevel() == ForecastRiskLevel.HIGH)
                .count();
        return new ForecastSummary(repository.findDemandSignals().size(), repository.findDemandForecasts().size(),
                repository.findReplenishmentPlans().size(), highRisk);
    }

    private ForecastRiskLevel riskByCoverage(int currentStock, int forecastQuantity) {
        if (forecastQuantity == 0 || currentStock >= forecastQuantity) {
            return ForecastRiskLevel.LOW;
        }
        return currentStock * 2 < forecastQuantity ? ForecastRiskLevel.HIGH : ForecastRiskLevel.MEDIUM;
    }

    private void requireNonNegative(int value, String message) {
        if (value < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "forecasting value must not be blank");
        }
        return normalized;
    }
}
