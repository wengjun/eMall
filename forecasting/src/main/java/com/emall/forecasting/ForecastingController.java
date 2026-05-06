package com.emall.forecasting;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecasting")
class ForecastingController {
    private final ForecastingService forecastingService;

    ForecastingController(ForecastingService forecastingService) {
        this.forecastingService = forecastingService;
    }

    @PostMapping("/demand-signals")
    ApiResponse<DemandSignal> recordDemandSignal(@Valid @RequestBody RecordDemandSignalRequest request) {
        return ApiResponse.ok(forecastingService.recordDemandSignal(request.skuId(), request.regionCode(),
                request.soldQuantity(), request.pageViews(), request.signalDate()));
    }

    @PostMapping("/demand-forecasts")
    ApiResponse<DemandForecast> buildDemandForecast(@Valid @RequestBody BuildDemandForecastRequest request) {
        return ApiResponse.ok(forecastingService.buildDemandForecast(request.skuId(), request.regionCode(),
                request.currentStock(), request.forecastDate()));
    }

    @PostMapping("/replenishment-plans")
    ApiResponse<ReplenishmentPlan> createReplenishmentPlan(
            @Valid @RequestBody CreateReplenishmentPlanRequest request) {
        return ApiResponse.ok(forecastingService.createReplenishmentPlan(request.skuId(), request.warehouseCode(),
                request.forecastQuantity(), request.availableStock(), request.planDate()));
    }

    @PostMapping("/capacity-forecasts")
    ApiResponse<CapacityForecast> createCapacityForecast(@Valid @RequestBody CreateCapacityForecastRequest request) {
        return ApiResponse.ok(forecastingService.createCapacityForecast(request.warehouseCode(),
                request.forecastOrders(), request.workerHours(), request.forecastDate()));
    }

    @GetMapping("/summary")
    ApiResponse<ForecastSummary> summary() {
        return ApiResponse.ok(forecastingService.summary());
    }

    record RecordDemandSignalRequest(@Positive long skuId, @NotBlank String regionCode, @Min(0) int soldQuantity,
                                     @Min(0) int pageViews, LocalDate signalDate) {
    }

    record BuildDemandForecastRequest(@Positive long skuId, @NotBlank String regionCode, @Min(0) int currentStock,
                                      LocalDate forecastDate) {
    }

    record CreateReplenishmentPlanRequest(@Positive long skuId, @NotBlank String warehouseCode,
                                          @Min(0) int forecastQuantity, @Min(0) int availableStock,
                                          LocalDate planDate) {
    }

    record CreateCapacityForecastRequest(@NotBlank String warehouseCode, @Min(0) int forecastOrders,
                                         @Min(0) int workerHours, LocalDate forecastDate) {
    }
}
