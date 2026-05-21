package com.emall.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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

@TableName("metric_definition")
record MetricDefinition(@TableId(value = "metric_id", type = IdType.INPUT) long metricId, String metricName,
        String owner, String expression, MetricStatus status, Instant createdAt, Instant updatedAt) {
    MetricDefinition changeStatus(MetricStatus nextStatus) {
        return new MetricDefinition(metricId, metricName, owner, expression, nextStatus, createdAt, Instant.now());
    }
}

@TableName("metric_point")
record MetricPoint(@TableId(value = "point_id", type = IdType.INPUT) long pointId, String metricName,
        String dimensionKey, BigDecimal metricValue, Instant eventTime, Instant createdAt) {
}

@TableName("dashboard_definition")
record DashboardDefinition(@TableId(value = "dashboard_id", type = IdType.INPUT) long dashboardId,
        String dashboardName, String businessDomain, String metricNames, Instant createdAt) {
}

@TableName("anomaly_signal")
record AnomalySignal(@TableId(value = "anomaly_id", type = IdType.INPUT) long anomalyId, String metricName,
        BigDecimal actualValue, BigDecimal expectedValue, String severity, Instant createdAt) {
}

@TableName("consent_record")
record ConsentRecord(@TableId(value = "consent_id", type = IdType.INPUT) long consentId, long userId, String purpose,
        boolean granted, Instant updatedAt) {
}

@TableName("privacy_request")
record PrivacyRequest(@TableId(value = "request_id", type = IdType.INPUT) long requestId, long userId,
        String requestType, PrivacyRequestStatus status, Instant createdAt, Instant updatedAt) {
    PrivacyRequest changeStatus(PrivacyRequestStatus nextStatus) {
        return new PrivacyRequest(requestId, userId, requestType, nextStatus, createdAt, Instant.now());
    }
}

record AnalyticsSummary(int approvedMetrics, int dashboards, int anomalies, int openPrivacyRequests) {
}
