package com.emall.experiment;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryExperimentRepository implements ExperimentRepository {
    private final ConcurrentMap<Long, ExperimentDefinition> experiments = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, GuardrailMetric> guardrails = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ExperimentMetric> metrics = new ConcurrentHashMap<>();

    @Override
    public ExperimentDefinition saveExperiment(ExperimentDefinition experiment) {
        experiments.put(experiment.experimentId(), experiment);
        return experiment;
    }

    @Override
    public Optional<ExperimentDefinition> findExperiment(long experimentId) {
        return Optional.ofNullable(experiments.get(experimentId));
    }

    @Override
    public List<ExperimentDefinition> findActiveByScene(String scene) {
        return experiments.values().stream().filter(experiment -> experiment.scene().equals(scene))
                .filter(experiment -> experiment.status() == ExperimentStatus.ACTIVE)
                .sorted(Comparator.comparing(ExperimentDefinition::updatedAt).reversed()).toList();
    }

    @Override
    public GuardrailMetric saveGuardrail(GuardrailMetric metric) {
        guardrails.put(metric.metricId(), metric);
        return metric;
    }

    @Override
    public List<GuardrailMetric> findGuardrails(long experimentId) {
        return guardrails.values().stream().filter(metric -> metric.experimentId() == experimentId).toList();
    }

    @Override
    public ExperimentMetric saveMetric(ExperimentMetric metric) {
        metrics.put(metric.metricRecordId(), metric);
        return metric;
    }

    @Override
    public List<ExperimentMetric> findMetrics(long experimentId) {
        return metrics.values().stream().filter(metric -> metric.experimentId() == experimentId).toList();
    }
}
