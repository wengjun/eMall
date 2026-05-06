package com.emall.analytics;

import java.util.List;
import java.util.Optional;

interface AnalyticsRepository {
    MetricDefinition saveMetric(MetricDefinition metric);

    Optional<MetricDefinition> findMetric(long metricId);

    List<MetricDefinition> findMetrics();

    MetricPoint saveMetricPoint(MetricPoint point);

    DashboardDefinition saveDashboard(DashboardDefinition dashboard);

    List<DashboardDefinition> findDashboards();

    AnomalySignal saveAnomaly(AnomalySignal anomaly);

    List<AnomalySignal> findAnomalies();

    ConsentRecord saveConsent(ConsentRecord consent);

    PrivacyRequest savePrivacyRequest(PrivacyRequest request);

    Optional<PrivacyRequest> findPrivacyRequest(long requestId);

    List<PrivacyRequest> findPrivacyRequests();
}
