package com.emall.analytics;

import static com.emall.common.persistence.RowMaps.decimalValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusAnalyticsRepository implements AnalyticsRepository {
    private final AnalyticsMapper analyticsMapper;

    MybatisPlusAnalyticsRepository(AnalyticsMapper analyticsMapper) {
        this.analyticsMapper = analyticsMapper;
    }

    @Override
    public MetricDefinition saveMetric(MetricDefinition metric) {
        analyticsMapper.saveMetric(metric);
        return metric;
    }

    @Override
    public Optional<MetricDefinition> findMetric(long metricId) {
        return Optional.ofNullable(analyticsMapper.findMetric(metricId)).map(this::mapMetric);
    }

    @Override
    public List<MetricDefinition> findMetrics() {
        return analyticsMapper.findMetrics().stream().map(this::mapMetric).toList();
    }

    @Override
    public MetricPoint saveMetricPoint(MetricPoint point) {
        analyticsMapper.saveMetricPoint(point);
        return point;
    }

    @Override
    public DashboardDefinition saveDashboard(DashboardDefinition dashboard) {
        analyticsMapper.saveDashboard(dashboard);
        return dashboard;
    }

    @Override
    public List<DashboardDefinition> findDashboards() {
        return analyticsMapper.findDashboards().stream().map(this::mapDashboard).toList();
    }

    @Override
    public AnomalySignal saveAnomaly(AnomalySignal anomaly) {
        analyticsMapper.saveAnomaly(anomaly);
        return anomaly;
    }

    @Override
    public List<AnomalySignal> findAnomalies() {
        return analyticsMapper.findAnomalies().stream().map(this::mapAnomaly).toList();
    }

    @Override
    public ConsentRecord saveConsent(ConsentRecord consent) {
        analyticsMapper.saveConsent(consent);
        return consent;
    }

    @Override
    public PrivacyRequest savePrivacyRequest(PrivacyRequest request) {
        analyticsMapper.savePrivacyRequest(request);
        return request;
    }

    @Override
    public Optional<PrivacyRequest> findPrivacyRequest(long requestId) {
        return Optional.ofNullable(analyticsMapper.findPrivacyRequest(requestId)).map(this::mapPrivacyRequest);
    }

    @Override
    public List<PrivacyRequest> findPrivacyRequests() {
        return analyticsMapper.findPrivacyRequests().stream().map(this::mapPrivacyRequest).toList();
    }

    private MetricDefinition mapMetric(Map<String, Object> row) {
        return new MetricDefinition(longValue(row, "metric_id"), stringValue(row, "metric_name"),
                stringValue(row, "owner"), stringValue(row, "expression"),
                MetricStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private DashboardDefinition mapDashboard(Map<String, Object> row) {
        return new DashboardDefinition(longValue(row, "dashboard_id"), stringValue(row, "dashboard_name"),
                stringValue(row, "business_domain"), stringValue(row, "metric_names"), instantValue(row, "created_at"));
    }

    private AnomalySignal mapAnomaly(Map<String, Object> row) {
        return new AnomalySignal(longValue(row, "anomaly_id"), stringValue(row, "metric_name"),
                decimalValue(row, "actual_value"), decimalValue(row, "expected_value"), stringValue(row, "severity"),
                instantValue(row, "created_at"));
    }

    private PrivacyRequest mapPrivacyRequest(Map<String, Object> row) {
        return new PrivacyRequest(longValue(row, "request_id"), longValue(row, "user_id"),
                stringValue(row, "request_type"), PrivacyRequestStatus.valueOf(stringValue(row, "status")),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }
}
