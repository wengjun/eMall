package com.emall.experiment;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusExperimentRepository implements ExperimentRepository {
    private final ExperimentMapper experimentMapper;
    private final ExperimentDefinitionMapper definitionMapper;
    private final GuardrailMetricMapper guardrailMapper;
    private final ExperimentMetricMapper metricMapper;

    MybatisPlusExperimentRepository(ExperimentMapper experimentMapper, ExperimentDefinitionMapper definitionMapper,
            GuardrailMetricMapper guardrailMapper, ExperimentMetricMapper metricMapper) {
        this.experimentMapper = experimentMapper;
        this.definitionMapper = definitionMapper;
        this.guardrailMapper = guardrailMapper;
        this.metricMapper = metricMapper;
    }

    @Override
    public ExperimentDefinition saveExperiment(ExperimentDefinition experiment) {
        experimentMapper.saveExperiment(experiment);
        return experiment;
    }

    @Override
    public Optional<ExperimentDefinition> findExperiment(long experimentId) {
        return Optional.ofNullable(definitionMapper.selectById(experimentId));
    }

    @Override
    public List<ExperimentDefinition> findActiveByScene(String scene) {
        QueryWrapper<ExperimentDefinition> query = new QueryWrapper<ExperimentDefinition>().eq("scene", scene)
                .eq("status", ExperimentStatus.ACTIVE.name()).orderByDesc("updated_at");
        return definitionMapper.selectList(query);
    }

    @Override
    public GuardrailMetric saveGuardrail(GuardrailMetric metric) {
        guardrailMapper.insert(metric);
        return metric;
    }

    @Override
    public List<GuardrailMetric> findGuardrails(long experimentId) {
        return guardrailMapper.selectList(new QueryWrapper<GuardrailMetric>().eq("experiment_id", experimentId));
    }

    @Override
    public ExperimentMetric saveMetric(ExperimentMetric metric) {
        metricMapper.insert(metric);
        return metric;
    }

    @Override
    public List<ExperimentMetric> findMetrics(long experimentId) {
        return metricMapper.selectList(new QueryWrapper<ExperimentMetric>().eq("experiment_id", experimentId));
    }
}
