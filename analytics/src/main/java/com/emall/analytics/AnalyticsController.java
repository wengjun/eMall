package com.emall.analytics;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
class AnalyticsController {
    private final AnalyticsService analyticsService;

    AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/metrics")
    ApiResponse<MetricDefinition> createMetric(@Valid @RequestBody CreateMetricRequest request) {
        return ApiResponse.ok(analyticsService.createMetric(request.metricName(), request.owner(),
                request.expression()));
    }

    @PatchMapping("/metrics/{metricId}/status")
    ApiResponse<MetricDefinition> changeMetricStatus(@PathVariable long metricId,
                                                     @Valid @RequestBody ChangeMetricStatusRequest request) {
        return ApiResponse.ok(analyticsService.changeMetricStatus(metricId, request.status()));
    }

    @PostMapping("/metric-points")
    ApiResponse<MetricPoint> recordMetricPoint(@Valid @RequestBody RecordMetricPointRequest request) {
        return ApiResponse.ok(analyticsService.recordMetricPoint(request.metricName(), request.dimensionKey(),
                request.metricValue(), request.eventTime()));
    }

    @PostMapping("/dashboards")
    ApiResponse<DashboardDefinition> createDashboard(@Valid @RequestBody CreateDashboardRequest request) {
        return ApiResponse.ok(analyticsService.createDashboard(request.dashboardName(), request.businessDomain(),
                request.metricNames()));
    }

    @PostMapping("/anomalies")
    ApiResponse<AnomalySignal> recordAnomaly(@Valid @RequestBody RecordAnomalyRequest request) {
        return ApiResponse.ok(analyticsService.recordAnomaly(request.metricName(), request.actualValue(),
                request.expectedValue(), request.severity()));
    }

    @PostMapping("/consents")
    ApiResponse<ConsentRecord> recordConsent(@Valid @RequestBody RecordConsentRequest request) {
        return ApiResponse.ok(analyticsService.recordConsent(request.userId(), request.purpose(),
                request.granted()));
    }

    @PostMapping("/privacy-requests")
    ApiResponse<PrivacyRequest> openPrivacyRequest(@Valid @RequestBody OpenPrivacyRequest request) {
        return ApiResponse.ok(analyticsService.openPrivacyRequest(request.userId(), request.requestType()));
    }

    @PatchMapping("/privacy-requests/{requestId}/status")
    ApiResponse<PrivacyRequest> changePrivacyRequestStatus(@PathVariable long requestId,
                                                           @Valid @RequestBody ChangePrivacyStatusRequest request) {
        return ApiResponse.ok(analyticsService.changePrivacyRequestStatus(requestId, request.status()));
    }

    @GetMapping("/summary")
    ApiResponse<AnalyticsSummary> summary() {
        return ApiResponse.ok(analyticsService.summary());
    }

    record CreateMetricRequest(@NotBlank String metricName, @NotBlank String owner,
                               @NotBlank String expression) {
    }

    record ChangeMetricStatusRequest(MetricStatus status) {
    }

    record RecordMetricPointRequest(@NotBlank String metricName, @NotBlank String dimensionKey,
                                    @DecimalMin("0.00") BigDecimal metricValue, Instant eventTime) {
    }

    record CreateDashboardRequest(@NotBlank String dashboardName, @NotBlank String businessDomain,
                                  @NotBlank String metricNames) {
    }

    record RecordAnomalyRequest(@NotBlank String metricName, BigDecimal actualValue, BigDecimal expectedValue,
                                @NotBlank String severity) {
    }

    record RecordConsentRequest(@Positive long userId, @NotBlank String purpose, boolean granted) {
    }

    record OpenPrivacyRequest(@Positive long userId, @NotBlank String requestType) {
    }

    record ChangePrivacyStatusRequest(PrivacyRequestStatus status) {
    }
}
