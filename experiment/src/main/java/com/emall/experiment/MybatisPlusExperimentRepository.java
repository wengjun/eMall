package com.emall.experiment;

import static com.emall.common.persistence.RowMaps.decimalValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusExperimentRepository implements ExperimentRepository {
    private final ExperimentMapper experimentMapper;

    MybatisPlusExperimentRepository(ExperimentMapper experimentMapper) {
        this.experimentMapper = experimentMapper;
    }

    @Override
    public ExperimentDefinition saveExperiment(ExperimentDefinition experiment) {
        experimentMapper.saveExperiment(experiment);
        return experiment;
    }

    @Override
    public Optional<ExperimentDefinition> findExperiment(long experimentId) {
        return Optional.ofNullable(experimentMapper.findExperiment(experimentId)).map(this::mapExperiment);
    }

    @Override
    public List<ExperimentDefinition> findActiveByScene(String scene) {
        return experimentMapper.findActiveByScene(scene).stream().map(this::mapExperiment).toList();
    }

    @Override
    public GuardrailMetric saveGuardrail(GuardrailMetric metric) {
        experimentMapper.saveGuardrail(metric);
        return metric;
    }

    @Override
    public List<GuardrailMetric> findGuardrails(long experimentId) {
        return experimentMapper.findGuardrails(experimentId).stream().map(this::mapGuardrail).toList();
    }

    @Override
    public ExperimentMetric saveMetric(ExperimentMetric metric) {
        experimentMapper.saveMetric(metric);
        return metric;
    }

    @Override
    public List<ExperimentMetric> findMetrics(long experimentId) {
        return experimentMapper.findMetrics(experimentId).stream().map(this::mapMetric).toList();
    }

    private ExperimentDefinition mapExperiment(Map<String, Object> row) {
        return new ExperimentDefinition(longValue(row, "experiment_id"), stringValue(row, "scene"),
                stringValue(row, "name"), stringValue(row, "mutual_exclusion_group"),
                intValue(row, "traffic_percent"), stringValue(row, "control_variant"),
                stringValue(row, "treatment_variant"), ExperimentStatus.valueOf(stringValue(row, "status")),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private GuardrailMetric mapGuardrail(Map<String, Object> row) {
        return new GuardrailMetric(longValue(row, "metric_id"), longValue(row, "experiment_id"),
                stringValue(row, "metric_name"), GuardrailDirection.valueOf(stringValue(row, "direction")),
                decimalValue(row, "threshold_value"), instantValue(row, "created_at"));
    }

    private ExperimentMetric mapMetric(Map<String, Object> row) {
        return new ExperimentMetric(longValue(row, "metric_record_id"), longValue(row, "experiment_id"),
                stringValue(row, "variant"), stringValue(row, "metric_name"), decimalValue(row, "metric_value"),
                instantValue(row, "recorded_at"));
    }
}
