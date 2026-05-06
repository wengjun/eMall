package com.emall.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AnalyticsServiceTest {
    private final InMemoryAnalyticsRepository repository = new InMemoryAnalyticsRepository();
    private final AnalyticsService service = new AnalyticsService(repository, new SnowflakeIdGenerator(54L));

    @Test
    void managesMetricsDashboardsAnomaliesAndPrivacyRequests() {
        MetricDefinition metric = service.createMetric("gmv", "bi", "sum(order_amount)");
        service.changeMetricStatus(metric.metricId(), MetricStatus.APPROVED);
        service.recordMetricPoint("gmv", "region=east", new BigDecimal("1000.00"), Instant.now());
        service.createDashboard("exec", "marketplace", "gmv");
        service.recordAnomaly("gmv", new BigDecimal("1000.00"), new BigDecimal("700.00"), "warning");
        service.recordConsent(1001L, "personalization", true);
        service.openPrivacyRequest(1001L, "delete");

        AnalyticsSummary summary = service.summary();

        assertThat(summary.approvedMetrics()).isEqualTo(1);
        assertThat(summary.dashboards()).isEqualTo(1);
        assertThat(summary.anomalies()).isEqualTo(1);
        assertThat(summary.openPrivacyRequests()).isEqualTo(1);
    }
}
