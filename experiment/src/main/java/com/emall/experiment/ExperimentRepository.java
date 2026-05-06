package com.emall.experiment;

import java.util.List;
import java.util.Optional;

interface ExperimentRepository {
    ExperimentDefinition saveExperiment(ExperimentDefinition experiment);

    Optional<ExperimentDefinition> findExperiment(long experimentId);

    List<ExperimentDefinition> findActiveByScene(String scene);

    GuardrailMetric saveGuardrail(GuardrailMetric metric);

    List<GuardrailMetric> findGuardrails(long experimentId);

    ExperimentMetric saveMetric(ExperimentMetric metric);

    List<ExperimentMetric> findMetrics(long experimentId);
}
