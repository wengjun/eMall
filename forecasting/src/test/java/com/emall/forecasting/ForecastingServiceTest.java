package com.emall.forecasting;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ForecastingServiceTest {
    private final InMemoryForecastingRepository repository = new InMemoryForecastingRepository();
    private final ForecastingService service = new ForecastingService(repository, new SnowflakeIdGenerator(44L));

    @Test
    void createsDemandReplenishmentAndCapacityForecasts() {
        service.recordDemandSignal(1001L, "east", 100, 1000, LocalDate.now().minusDays(1));
        service.recordDemandSignal(1001L, "east", 120, 1200, LocalDate.now());
        DemandForecast forecast = service.buildDemandForecast(1001L, "east", 30, LocalDate.now().plusDays(1));
        ReplenishmentPlan plan = service.createReplenishmentPlan(1001L, "WH-EAST", forecast.forecastQuantity(), 30,
                LocalDate.now().plusDays(1));
        CapacityForecast capacity = service.createCapacityForecast("WH-EAST", 1000, 20, LocalDate.now().plusDays(1));

        assertThat(forecast.stockoutRisk()).isEqualTo(ForecastRiskLevel.HIGH);
        assertThat(plan.priority()).isEqualTo(ForecastRiskLevel.HIGH);
        assertThat(capacity.pressureLevel()).isEqualTo(ForecastRiskLevel.HIGH);
        assertThat(service.summary().highRiskCount()).isEqualTo(3);
    }
}
