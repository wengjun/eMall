package com.emall.analytics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcAnalyticsRepository implements AnalyticsRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MetricDefinition saveMetric(MetricDefinition metric) {
        jdbcTemplate.update("""
                INSERT INTO metric_definition
                    (metric_id, metric_name, owner, expression, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, metric.metricId(), metric.metricName(), metric.owner(), metric.expression(),
                metric.status().name(), Timestamp.from(metric.createdAt()), Timestamp.from(metric.updatedAt()));
        return metric;
    }

    @Override
    public Optional<MetricDefinition> findMetric(long metricId) {
        return jdbcTemplate.query("SELECT * FROM metric_definition WHERE metric_id = ?", this::mapMetric, metricId)
                .stream().findFirst();
    }

    @Override
    public List<MetricDefinition> findMetrics() {
        return jdbcTemplate.query("SELECT * FROM metric_definition", this::mapMetric);
    }

    @Override
    public MetricPoint saveMetricPoint(MetricPoint point) {
        jdbcTemplate.update("""
                INSERT INTO metric_point
                    (point_id, metric_name, dimension_key, metric_value, event_time, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, point.pointId(), point.metricName(), point.dimensionKey(), point.metricValue(),
                Timestamp.from(point.eventTime()), Timestamp.from(point.createdAt()));
        return point;
    }

    @Override
    public DashboardDefinition saveDashboard(DashboardDefinition dashboard) {
        jdbcTemplate.update("""
                INSERT INTO dashboard_definition
                    (dashboard_id, dashboard_name, business_domain, metric_names, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, dashboard.dashboardId(), dashboard.dashboardName(), dashboard.businessDomain(),
                dashboard.metricNames(), Timestamp.from(dashboard.createdAt()));
        return dashboard;
    }

    @Override
    public List<DashboardDefinition> findDashboards() {
        return jdbcTemplate.query("SELECT * FROM dashboard_definition", this::mapDashboard);
    }

    @Override
    public AnomalySignal saveAnomaly(AnomalySignal anomaly) {
        jdbcTemplate.update("""
                INSERT INTO anomaly_signal
                    (anomaly_id, metric_name, actual_value, expected_value, severity, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, anomaly.anomalyId(), anomaly.metricName(), anomaly.actualValue(), anomaly.expectedValue(),
                anomaly.severity(), Timestamp.from(anomaly.createdAt()));
        return anomaly;
    }

    @Override
    public List<AnomalySignal> findAnomalies() {
        return jdbcTemplate.query("SELECT * FROM anomaly_signal", this::mapAnomaly);
    }

    @Override
    public ConsentRecord saveConsent(ConsentRecord consent) {
        jdbcTemplate.update("""
                INSERT INTO consent_record (consent_id, user_id, purpose, granted, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE granted = VALUES(granted), updated_at = VALUES(updated_at)
                """, consent.consentId(), consent.userId(), consent.purpose(), consent.granted(),
                Timestamp.from(consent.updatedAt()));
        return consent;
    }

    @Override
    public PrivacyRequest savePrivacyRequest(PrivacyRequest request) {
        jdbcTemplate.update("""
                INSERT INTO privacy_request
                    (request_id, user_id, request_type, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, request.requestId(), request.userId(), request.requestType(), request.status().name(),
                Timestamp.from(request.createdAt()), Timestamp.from(request.updatedAt()));
        return request;
    }

    @Override
    public Optional<PrivacyRequest> findPrivacyRequest(long requestId) {
        return jdbcTemplate.query("SELECT * FROM privacy_request WHERE request_id = ?", this::mapPrivacyRequest,
                requestId).stream().findFirst();
    }

    @Override
    public List<PrivacyRequest> findPrivacyRequests() {
        return jdbcTemplate.query("SELECT * FROM privacy_request", this::mapPrivacyRequest);
    }

    private MetricDefinition mapMetric(ResultSet rs, int rowNum) throws SQLException {
        return new MetricDefinition(rs.getLong("metric_id"), rs.getString("metric_name"), rs.getString("owner"),
                rs.getString("expression"), MetricStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private DashboardDefinition mapDashboard(ResultSet rs, int rowNum) throws SQLException {
        return new DashboardDefinition(rs.getLong("dashboard_id"), rs.getString("dashboard_name"),
                rs.getString("business_domain"), rs.getString("metric_names"),
                rs.getTimestamp("created_at").toInstant());
    }

    private AnomalySignal mapAnomaly(ResultSet rs, int rowNum) throws SQLException {
        return new AnomalySignal(rs.getLong("anomaly_id"), rs.getString("metric_name"),
                rs.getBigDecimal("actual_value"), rs.getBigDecimal("expected_value"), rs.getString("severity"),
                rs.getTimestamp("created_at").toInstant());
    }

    private PrivacyRequest mapPrivacyRequest(ResultSet rs, int rowNum) throws SQLException {
        return new PrivacyRequest(rs.getLong("request_id"), rs.getLong("user_id"),
                rs.getString("request_type"), PrivacyRequestStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
