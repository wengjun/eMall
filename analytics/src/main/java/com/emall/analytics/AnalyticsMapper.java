package com.emall.analytics;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
