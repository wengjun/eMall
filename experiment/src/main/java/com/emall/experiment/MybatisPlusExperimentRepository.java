package com.emall.experiment;

import java.util.List;
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
        return Optional.ofNullable(experimentMapper.findExperiment(experimentId));
    }

    @Override
    public List<ExperimentDefinition> findActiveByScene(String scene) {
        return experimentMapper.findActiveByScene(scene);
    }

    @Override
    public GuardrailMetric saveGuardrail(GuardrailMetric metric) {
        experimentMapper.saveGuardrail(metric);
        return metric;
    }

    @Override
    public List<GuardrailMetric> findGuardrails(long experimentId) {
        return experimentMapper.findGuardrails(experimentId);
    }

    @Override
    public ExperimentMetric saveMetric(ExperimentMetric metric) {
        experimentMapper.saveMetric(metric);
        return metric;
    }

    @Override
    public List<ExperimentMetric> findMetrics(long experimentId) {
        return experimentMapper.findMetrics(experimentId);
    }
}
