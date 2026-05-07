package com.emall.analytics;

import java.math.BigDecimal;
import java.time.Instant;

enum MetricStatus {
    DRAFT,
    APPROVED,
    DEPRECATED
}

enum PrivacyRequestStatus {
    OPEN,
    COMPLETED,
    REJECTED
}

record MetricDefinition(long metricId, String metricName, String owner, String expression, MetricStatus status,
        Instant createdAt, Instant updatedAt) {
    MetricDefinition changeStatus(MetricStatus nextStatus) {
        return new MetricDefinition(metricId, metricName, owner, expression, nextStatus, createdAt, Instant.now());
    }
}

record MetricPoint(long pointId, String metricName, String dimensionKey, BigDecimal metricValue, Instant eventTime,
        Instant createdAt) {
}

record DashboardDefinition(long dashboardId, String dashboardName, String businessDomain, String metricNames,
        Instant createdAt) {
}

record AnomalySignal(long anomalyId, String metricName, BigDecimal actualValue, BigDecimal expectedValue,
        String severity, Instant createdAt) {
}

record ConsentRecord(long consentId, long userId, String purpose, boolean granted, Instant updatedAt) {
}

record PrivacyRequest(long requestId, long userId, String requestType, PrivacyRequestStatus status, Instant createdAt,
        Instant updatedAt) {
    PrivacyRequest changeStatus(PrivacyRequestStatus nextStatus) {
        return new PrivacyRequest(requestId, userId, requestType, nextStatus, createdAt, Instant.now());
    }
}

record AnalyticsSummary(int approvedMetrics, int dashboards, int anomalies, int openPrivacyRequests) {
}
