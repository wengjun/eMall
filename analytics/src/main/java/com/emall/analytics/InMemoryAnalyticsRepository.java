package com.emall.analytics;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryAnalyticsRepository implements AnalyticsRepository {
    private final ConcurrentMap<Long, MetricDefinition> metrics = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, MetricPoint> points = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, DashboardDefinition> dashboards = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AnomalySignal> anomalies = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ConsentRecord> consents = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PrivacyRequest> privacyRequests = new ConcurrentHashMap<>();

    @Override
    public MetricDefinition saveMetric(MetricDefinition metric) {
        metrics.put(metric.metricId(), metric);
        return metric;
    }

    @Override
    public Optional<MetricDefinition> findMetric(long metricId) {
        return Optional.ofNullable(metrics.get(metricId));
    }

    @Override
    public List<MetricDefinition> findMetrics() {
        return List.copyOf(metrics.values());
    }

    @Override
    public MetricPoint saveMetricPoint(MetricPoint point) {
        points.put(point.pointId(), point);
        return point;
    }

    @Override
    public DashboardDefinition saveDashboard(DashboardDefinition dashboard) {
        dashboards.put(dashboard.dashboardId(), dashboard);
        return dashboard;
    }

    @Override
    public List<DashboardDefinition> findDashboards() {
        return List.copyOf(dashboards.values());
    }

    @Override
    public AnomalySignal saveAnomaly(AnomalySignal anomaly) {
        anomalies.put(anomaly.anomalyId(), anomaly);
        return anomaly;
    }

    @Override
    public List<AnomalySignal> findAnomalies() {
        return List.copyOf(anomalies.values());
    }

    @Override
    public ConsentRecord saveConsent(ConsentRecord consent) {
        consents.put(consent.consentId(), consent);
        return consent;
    }

    @Override
    public PrivacyRequest savePrivacyRequest(PrivacyRequest request) {
        privacyRequests.put(request.requestId(), request);
        return request;
    }

    @Override
    public Optional<PrivacyRequest> findPrivacyRequest(long requestId) {
        return Optional.ofNullable(privacyRequests.get(requestId));
    }

    @Override
    public List<PrivacyRequest> findPrivacyRequests() {
        return List.copyOf(privacyRequests.values());
    }
}
