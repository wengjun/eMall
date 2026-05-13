package com.emall.experiment;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface ExperimentMapper {
    @Insert("""
            INSERT INTO experiment_definition
                (experiment_id, scene, name, mutual_exclusion_group, traffic_percent, control_variant,
                treatment_variant, status, created_at, updated_at)
            VALUES (#{experiment.experimentId}, #{experiment.scene}, #{experiment.name},
                #{experiment.mutualExclusionGroup}, #{experiment.trafficPercent}, #{experiment.controlVariant},
                #{experiment.treatmentVariant}, #{experiment.status}, #{experiment.createdAt},
                #{experiment.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveExperiment(@Param("experiment") ExperimentDefinition experiment);

    @Select("""
            SELECT experiment_id, scene, name, mutual_exclusion_group, traffic_percent, control_variant,
                treatment_variant, status, created_at, updated_at
            FROM experiment_definition
            WHERE experiment_id = #{experimentId}
            """)
    ExperimentDefinition findExperiment(@Param("experimentId") long experimentId);

    @Select("""
            SELECT experiment_id, scene, name, mutual_exclusion_group, traffic_percent, control_variant,
                treatment_variant, status, created_at, updated_at
            FROM experiment_definition
            WHERE scene = #{scene} AND status = 'ACTIVE'
            ORDER BY updated_at DESC
            """)
    List<ExperimentDefinition> findActiveByScene(@Param("scene") String scene);

    @Insert("""
            INSERT INTO experiment_guardrail
                (metric_id, experiment_id, metric_name, direction, threshold_value, created_at)
            VALUES (#{metric.metricId}, #{metric.experimentId}, #{metric.metricName}, #{metric.direction},
                #{metric.threshold}, #{metric.createdAt})
            """)
    int saveGuardrail(@Param("metric") GuardrailMetric metric);

    @Select("""
            SELECT metric_id, experiment_id, metric_name, direction, threshold_value, created_at
            FROM experiment_guardrail
            WHERE experiment_id = #{experimentId}
            """)
    List<GuardrailMetric> findGuardrails(@Param("experimentId") long experimentId);

    @Insert("""
            INSERT INTO experiment_metric
                (metric_record_id, experiment_id, variant, metric_name, metric_value, recorded_at)
            VALUES (#{metric.metricRecordId}, #{metric.experimentId}, #{metric.variant}, #{metric.metricName},
                #{metric.value}, #{metric.recordedAt})
            """)
    int saveMetric(@Param("metric") ExperimentMetric metric);

    @Select("""
            SELECT metric_record_id, experiment_id, variant, metric_name, metric_value, recorded_at
            FROM experiment_metric
            WHERE experiment_id = #{experimentId}
            """)
    List<ExperimentMetric> findMetrics(@Param("experimentId") long experimentId);
}
