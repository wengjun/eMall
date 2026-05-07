package com.emall.experiment;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ExperimentService {
    private final ExperimentRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    ExperimentService(ExperimentRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    ExperimentDefinition createExperiment(String scene, String name, String mutualExclusionGroup, int trafficPercent,
            String controlVariant, String treatmentVariant) {
        if (trafficPercent < 0 || trafficPercent > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "traffic percent must be 0-100");
        }
        Instant now = Instant.now();
        return repository.saveExperiment(new ExperimentDefinition(idGenerator.nextId(), normalize(scene), name,
                normalize(mutualExclusionGroup), trafficPercent, normalize(controlVariant), normalize(treatmentVariant),
                ExperimentStatus.DRAFT, now, now));
    }

    @Transactional
    ExperimentDefinition changeStatus(long experimentId, ExperimentStatus status) {
        ExperimentDefinition experiment = requireExperiment(experimentId);
        return repository.saveExperiment(experiment.changeStatus(status));
    }

    @Transactional
    GuardrailMetric addGuardrail(long experimentId, String metricName, GuardrailDirection direction,
            BigDecimal threshold) {
        requireExperiment(experimentId);
        return repository.saveGuardrail(new GuardrailMetric(idGenerator.nextId(), experimentId, normalize(metricName),
                direction, threshold, Instant.now()));
    }

    ExperimentAssignment assign(String scene, String userKey) {
        List<ExperimentDefinition> experiments = repository.findActiveByScene(normalize(scene));
        if (experiments.isEmpty()) {
            return new ExperimentAssignment(0L, normalize(scene), userKey, "control", "default");
        }
        ExperimentDefinition experiment = experiments.get(0);
        int bucketValue = Math.floorMod((experiment.mutualExclusionGroup() + ":" + userKey).hashCode(), 100);
        String variant =
                bucketValue < experiment.trafficPercent() ? experiment.treatmentVariant() : experiment.controlVariant();
        return new ExperimentAssignment(experiment.experimentId(), experiment.scene(), userKey, variant,
                "bucket-" + bucketValue);
    }

    @Transactional
    ExperimentMetric recordMetric(long experimentId, String variant, String metricName, BigDecimal value) {
        requireExperiment(experimentId);
        return repository.saveMetric(new ExperimentMetric(idGenerator.nextId(), experimentId, normalize(variant),
                normalize(metricName), value, Instant.now()));
    }

    @Transactional
    ExperimentReport report(long experimentId, String metricName) {
        ExperimentDefinition experiment = requireExperiment(experimentId);
        List<ExperimentMetric> metrics = repository.findMetrics(experimentId).stream()
                .filter(metric -> metric.metricName().equals(normalize(metricName))).toList();
        BigDecimal control = average(metrics, experiment.controlVariant());
        BigDecimal treatment = average(metrics, experiment.treatmentVariant());
        boolean breached = guardrailBreached(experimentId, normalize(metricName), treatment);
        if (breached && experiment.status() == ExperimentStatus.ACTIVE) {
            repository.saveExperiment(experiment.changeStatus(ExperimentStatus.ROLLED_BACK));
        }
        return new ExperimentReport(experimentId, control, treatment, breached, breached);
    }

    private boolean guardrailBreached(long experimentId, String metricName, BigDecimal value) {
        return repository.findGuardrails(experimentId).stream()
                .filter(guardrail -> guardrail.metricName().equals(metricName))
                .anyMatch(guardrail -> guardrail.direction() == GuardrailDirection.MAX
                        ? value.compareTo(guardrail.threshold()) > 0
                        : value.compareTo(guardrail.threshold()) < 0);
    }

    private BigDecimal average(List<ExperimentMetric> metrics, String variant) {
        List<BigDecimal> values = metrics.stream().filter(metric -> metric.variant().equals(variant))
                .map(ExperimentMetric::value).toList();
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    private ExperimentDefinition requireExperiment(long experimentId) {
        return repository.findExperiment(experimentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "experiment not found"));
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "experiment value must not be blank");
        }
        return normalized;
    }
}
