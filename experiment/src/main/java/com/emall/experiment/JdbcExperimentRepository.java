package com.emall.experiment;

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
class JdbcExperimentRepository implements ExperimentRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcExperimentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ExperimentDefinition saveExperiment(ExperimentDefinition experiment) {
        jdbcTemplate.update("""
                INSERT INTO experiment_definition
                    (experiment_id, scene, name, mutual_exclusion_group, traffic_percent, control_variant,
                    treatment_variant, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, experiment.experimentId(), experiment.scene(), experiment.name(),
                experiment.mutualExclusionGroup(), experiment.trafficPercent(), experiment.controlVariant(),
                experiment.treatmentVariant(), experiment.status().name(), Timestamp.from(experiment.createdAt()),
                Timestamp.from(experiment.updatedAt()));
        return experiment;
    }

    @Override
    public Optional<ExperimentDefinition> findExperiment(long experimentId) {
        return jdbcTemplate
                .query("SELECT * FROM experiment_definition WHERE experiment_id = ?", this::mapExperiment, experimentId)
                .stream().findFirst();
    }

    @Override
    public List<ExperimentDefinition> findActiveByScene(String scene) {
        return jdbcTemplate.query("""
                SELECT * FROM experiment_definition
                WHERE scene = ? AND status = 'ACTIVE'
                ORDER BY updated_at DESC
                """, this::mapExperiment, scene);
    }

    @Override
    public GuardrailMetric saveGuardrail(GuardrailMetric metric) {
        jdbcTemplate.update("""
                INSERT INTO experiment_guardrail
                    (metric_id, experiment_id, metric_name, direction, threshold_value, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, metric.metricId(), metric.experimentId(), metric.metricName(), metric.direction().name(),
                metric.threshold(), Timestamp.from(metric.createdAt()));
        return metric;
    }

    @Override
    public List<GuardrailMetric> findGuardrails(long experimentId) {
        return jdbcTemplate.query("SELECT * FROM experiment_guardrail WHERE experiment_id = ?", this::mapGuardrail,
                experimentId);
    }

    @Override
    public ExperimentMetric saveMetric(ExperimentMetric metric) {
        jdbcTemplate.update("""
                INSERT INTO experiment_metric
                    (metric_record_id, experiment_id, variant, metric_name, metric_value, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, metric.metricRecordId(), metric.experimentId(), metric.variant(), metric.metricName(),
                metric.value(), Timestamp.from(metric.recordedAt()));
        return metric;
    }

    @Override
    public List<ExperimentMetric> findMetrics(long experimentId) {
        return jdbcTemplate.query("SELECT * FROM experiment_metric WHERE experiment_id = ?", this::mapMetric,
                experimentId);
    }

    private ExperimentDefinition mapExperiment(ResultSet rs, int rowNum) throws SQLException {
        return new ExperimentDefinition(rs.getLong("experiment_id"), rs.getString("scene"), rs.getString("name"),
                rs.getString("mutual_exclusion_group"), rs.getInt("traffic_percent"), rs.getString("control_variant"),
                rs.getString("treatment_variant"), ExperimentStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private GuardrailMetric mapGuardrail(ResultSet rs, int rowNum) throws SQLException {
        return new GuardrailMetric(rs.getLong("metric_id"), rs.getLong("experiment_id"), rs.getString("metric_name"),
                GuardrailDirection.valueOf(rs.getString("direction")), rs.getBigDecimal("threshold_value"),
                rs.getTimestamp("created_at").toInstant());
    }

    private ExperimentMetric mapMetric(ResultSet rs, int rowNum) throws SQLException {
        return new ExperimentMetric(rs.getLong("metric_record_id"), rs.getLong("experiment_id"),
                rs.getString("variant"), rs.getString("metric_name"), rs.getBigDecimal("metric_value"),
                rs.getTimestamp("recorded_at").toInstant());
    }
}
