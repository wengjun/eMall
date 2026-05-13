package com.emall.analytics;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface AnalyticsMapper {
    @Insert("""
            INSERT INTO metric_definition
                (metric_id, metric_name, owner, expression, status, created_at, updated_at)
            VALUES (#{metric.metricId}, #{metric.metricName}, #{metric.owner}, #{metric.expression},
                #{metric.status}, #{metric.createdAt}, #{metric.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveMetric(@Param("metric") MetricDefinition metric);

    @Select("""
            SELECT metric_id, metric_name, owner, expression, status, created_at, updated_at
            FROM metric_definition
            WHERE metric_id = #{metricId}
            """)
    MetricDefinition findMetric(@Param("metricId") long metricId);

    @Select("""
            SELECT metric_id, metric_name, owner, expression, status, created_at, updated_at
            FROM metric_definition
            """)
    List<MetricDefinition> findMetrics();

    @Insert("""
            INSERT INTO metric_point
                (point_id, metric_name, dimension_key, metric_value, event_time, created_at)
            VALUES (#{point.pointId}, #{point.metricName}, #{point.dimensionKey}, #{point.metricValue},
                #{point.eventTime}, #{point.createdAt})
            """)
    int saveMetricPoint(@Param("point") MetricPoint point);

    @Insert("""
            INSERT INTO dashboard_definition
                (dashboard_id, dashboard_name, business_domain, metric_names, created_at)
            VALUES (#{dashboard.dashboardId}, #{dashboard.dashboardName}, #{dashboard.businessDomain},
                #{dashboard.metricNames}, #{dashboard.createdAt})
            """)
    int saveDashboard(@Param("dashboard") DashboardDefinition dashboard);

    @Select("""
            SELECT dashboard_id, dashboard_name, business_domain, metric_names, created_at
            FROM dashboard_definition
            """)
    List<DashboardDefinition> findDashboards();

    @Insert("""
            INSERT INTO anomaly_signal
                (anomaly_id, metric_name, actual_value, expected_value, severity, created_at)
            VALUES (#{anomaly.anomalyId}, #{anomaly.metricName}, #{anomaly.actualValue},
                #{anomaly.expectedValue}, #{anomaly.severity}, #{anomaly.createdAt})
            """)
    int saveAnomaly(@Param("anomaly") AnomalySignal anomaly);

    @Select("""
            SELECT anomaly_id, metric_name, actual_value, expected_value, severity, created_at
            FROM anomaly_signal
            """)
    List<AnomalySignal> findAnomalies();

    @Insert("""
            INSERT INTO consent_record (consent_id, user_id, purpose, granted, updated_at)
            VALUES (#{consent.consentId}, #{consent.userId}, #{consent.purpose}, #{consent.granted},
                #{consent.updatedAt})
            ON DUPLICATE KEY UPDATE granted = VALUES(granted), updated_at = VALUES(updated_at)
            """)
    int saveConsent(@Param("consent") ConsentRecord consent);

    @Insert("""
            INSERT INTO privacy_request
                (request_id, user_id, request_type, status, created_at, updated_at)
            VALUES (#{request.requestId}, #{request.userId}, #{request.requestType}, #{request.status},
                #{request.createdAt}, #{request.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int savePrivacyRequest(@Param("request") PrivacyRequest request);

    @Select("""
            SELECT request_id, user_id, request_type, status, created_at, updated_at
            FROM privacy_request
            WHERE request_id = #{requestId}
            """)
    PrivacyRequest findPrivacyRequest(@Param("requestId") long requestId);

    @Select("""
            SELECT request_id, user_id, request_type, status, created_at, updated_at
            FROM privacy_request
            """)
    List<PrivacyRequest> findPrivacyRequests();
}
