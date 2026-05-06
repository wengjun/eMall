package com.emall.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ExperimentServiceTest {
    private final InMemoryExperimentRepository repository = new InMemoryExperimentRepository();
    private final ExperimentService service = new ExperimentService(repository, new SnowflakeIdGenerator(33L));

    @Test
    void assignsTrafficAndRollsBackOnGuardrailBreach() {
        ExperimentDefinition experiment = service.createExperiment("search", "rank-v2", "search-rank",
                100, "control", "treatment");
        service.addGuardrail(experiment.experimentId(), "http_5xx_rate", GuardrailDirection.MAX,
                new BigDecimal("0.010000"));
        service.changeStatus(experiment.experimentId(), ExperimentStatus.ACTIVE);

        ExperimentAssignment assignment = service.assign("search", "user-1");
        service.recordMetric(experiment.experimentId(), "control", "http_5xx_rate", new BigDecimal("0.001"));
        service.recordMetric(experiment.experimentId(), "treatment", "http_5xx_rate", new BigDecimal("0.020"));
        ExperimentReport report = service.report(experiment.experimentId(), "http_5xx_rate");

        assertThat(assignment.variant()).isEqualTo("treatment");
        assertThat(report.rollbackRecommended()).isTrue();
    }
}
