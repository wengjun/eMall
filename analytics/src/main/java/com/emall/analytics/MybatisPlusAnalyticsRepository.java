package com.emall.analytics;

import java.util.List;
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
        return Optional.ofNullable(analyticsMapper.findMetric(metricId));
    }

    @Override
    public List<MetricDefinition> findMetrics() {
        return analyticsMapper.findMetrics();
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
        return analyticsMapper.findDashboards();
    }

    @Override
    public AnomalySignal saveAnomaly(AnomalySignal anomaly) {
        analyticsMapper.saveAnomaly(anomaly);
        return anomaly;
    }

    @Override
    public List<AnomalySignal> findAnomalies() {
        return analyticsMapper.findAnomalies();
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
        return Optional.ofNullable(analyticsMapper.findPrivacyRequest(requestId));
    }

    @Override
    public List<PrivacyRequest> findPrivacyRequests() {
        return analyticsMapper.findPrivacyRequests();
    }
}
