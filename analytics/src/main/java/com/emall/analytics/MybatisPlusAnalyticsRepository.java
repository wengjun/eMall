package com.emall.analytics;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusAnalyticsRepository implements AnalyticsRepository {
    private final AnalyticsMapper analyticsMapper;
    private final MetricDefinitionMapper metricMapper;
    private final MetricPointMapper metricPointMapper;
    private final DashboardDefinitionMapper dashboardMapper;
    private final AnomalySignalMapper anomalyMapper;
    private final PrivacyRequestMapper privacyRequestMapper;

    MybatisPlusAnalyticsRepository(AnalyticsMapper analyticsMapper, MetricDefinitionMapper metricMapper,
            MetricPointMapper metricPointMapper, DashboardDefinitionMapper dashboardMapper,
            AnomalySignalMapper anomalyMapper, PrivacyRequestMapper privacyRequestMapper) {
        this.analyticsMapper = analyticsMapper;
        this.metricMapper = metricMapper;
        this.metricPointMapper = metricPointMapper;
        this.dashboardMapper = dashboardMapper;
        this.anomalyMapper = anomalyMapper;
        this.privacyRequestMapper = privacyRequestMapper;
    }

    @Override
    public MetricDefinition saveMetric(MetricDefinition metric) {
        analyticsMapper.saveMetric(metric);
        return metric;
    }

    @Override
    public Optional<MetricDefinition> findMetric(long metricId) {
        return Optional.ofNullable(metricMapper.selectById(metricId));
    }

    @Override
    public List<MetricDefinition> findMetrics() {
        return metricMapper.selectList(null);
    }

    @Override
    public MetricPoint saveMetricPoint(MetricPoint point) {
        metricPointMapper.insert(point);
        return point;
    }

    @Override
    public DashboardDefinition saveDashboard(DashboardDefinition dashboard) {
        dashboardMapper.insert(dashboard);
        return dashboard;
    }

    @Override
    public List<DashboardDefinition> findDashboards() {
        return dashboardMapper.selectList(null);
    }

    @Override
    public AnomalySignal saveAnomaly(AnomalySignal anomaly) {
        anomalyMapper.insert(anomaly);
        return anomaly;
    }

    @Override
    public List<AnomalySignal> findAnomalies() {
        return anomalyMapper.selectList(null);
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
        return Optional.ofNullable(privacyRequestMapper.selectById(requestId));
    }

    @Override
    public List<PrivacyRequest> findPrivacyRequests() {
        return privacyRequestMapper.selectList(null);
    }
}
