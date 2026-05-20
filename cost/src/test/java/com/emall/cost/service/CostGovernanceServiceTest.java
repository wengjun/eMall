package com.emall.cost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.cost.domain.CapacityRiskLevel;
import com.emall.cost.domain.CapacitySummary;
import com.emall.cost.domain.CostActionStatus;
import com.emall.cost.domain.CostActionType;
import com.emall.cost.domain.CostBudget;
import com.emall.cost.domain.CostOptimizationAction;
import com.emall.cost.domain.CostSignalType;
import com.emall.cost.domain.CostSummary;
import com.emall.cost.repository.InMemoryCostRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CostGovernanceServiceTest {
    private final InMemoryCostRepository repository = new InMemoryCostRepository();
    private final CostGovernanceService service = new CostGovernanceService(repository, new SnowflakeIdGenerator(17L));

    @Test
    void opensActionWhenCacheHitRatioFallsBelowThreshold() {
        service.recordSignal("search", CostSignalType.CACHE_HIT_RATIO, new BigDecimal("0.72"), new BigDecimal("0.85"),
                Instant.now());
        service.recordSignal("search", CostSignalType.CACHE_HIT_RATIO, new BigDecimal("0.70"), new BigDecimal("0.85"),
                Instant.now());

        List<CostOptimizationAction> actions = service.findActiveActions("search");

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).actionType()).isEqualTo(CostActionType.INCREASE_CACHE_TTL);
        assertThat(actions.get(0).priority()).isEqualTo(1);
    }

    @Test
    void opensActionWhenIndexStorageExceedsThreshold() {
        service.recordSignal("search", CostSignalType.INDEX_STORAGE_GB, new BigDecimal("650"), new BigDecimal("500"),
                Instant.now());

        List<CostOptimizationAction> actions = service.findActiveActions("search");

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).actionType()).isEqualTo(CostActionType.ROLLOVER_INDEX);
    }

    @Test
    void budgetSummaryFlagsAlertThreshold() {
        CostBudget budget =
                service.upsertBudget("export", new BigDecimal("10000"), new BigDecimal("8500"), "usd", 80, true);

        CostSummary summary = service.summary("export");

        assertThat(budget.currency()).isEqualTo("USD");
        assertThat(summary.budgetAlert()).isTrue();
        assertThat(summary.monthlyBudget()).isEqualByComparingTo("10000.000000");
    }

    @Test
    void actionStatusCanBeChanged() {
        service.recordSignal("fulfillment", CostSignalType.HOT_STORAGE_GB, new BigDecimal("1200"),
                new BigDecimal("1000"), Instant.now());
        long actionId = service.findActiveActions("fulfillment").get(0).actionId();

        CostOptimizationAction completed = service.changeActionStatus(actionId, CostActionStatus.COMPLETED);

        assertThat(completed.status()).isEqualTo(CostActionStatus.COMPLETED);
        assertThat(service.findActiveActions("fulfillment")).isEmpty();
    }

    @Test
    void recordsCapacityBaselineAndFlagsHpaRisk() {
        service.recordCapacityBaseline("order", 100000, 120000, 85000, 9, 10, new BigDecimal("0.72"),
                new BigDecimal("0.68"), new BigDecimal("12000"), true, Instant.now());

        CapacitySummary summary = service.capacitySummary("order");
        List<CostOptimizationAction> actions = service.findActiveActions("order");

        assertThat(summary.riskLevel()).isEqualTo(CapacityRiskLevel.HPA_NEAR_LIMIT);
        assertThat(summary.hpaWatermark()).isEqualByComparingTo("0.900000");
        assertThat(actions).singleElement()
                .satisfies(action -> assertThat(action.actionType()).isEqualTo(CostActionType.REVIEW_HPA_LIMIT));
    }

    @Test
    void marksIdleCapacityWithSloSafeRecommendation() {
        service.recordCapacityBaseline("search", 100000, 120000, 10000, 6, 20, new BigDecimal("0.20"),
                new BigDecimal("0.22"), new BigDecimal("8000"), true, Instant.now());

        CapacitySummary summary = service.capacitySummary("search");

        assertThat(summary.riskLevel()).isEqualTo(CapacityRiskLevel.IDLE_RESOURCE);
        assertThat(summary.recommendation()).contains("SLO guard");
        assertThat(service.findActiveActions("search")).singleElement()
                .satisfies(action -> assertThat(action.actionType()).isEqualTo(CostActionType.REDUCE_IDLE_REPLICAS));
    }

    @Test
    void flagsScaleOutRequiredBeforeTrafficExceedsSafeEnvelope() {
        service.recordCapacityBaseline("payment", 100000, 150000, 95000, 8, 20, new BigDecimal("0.65"),
                new BigDecimal("0.62"), new BigDecimal("18000"), true, Instant.now());

        CapacitySummary summary = service.capacitySummary("payment");

        assertThat(summary.riskLevel()).isEqualTo(CapacityRiskLevel.SCALE_OUT_REQUIRED);
        assertThat(summary.recommendation()).contains("Scale out");
        assertThat(service.findActiveActions("payment")).singleElement()
                .satisfies(action -> assertThat(action.actionType()).isEqualTo(CostActionType.SCALE_SERVICE_REPLICAS));
    }

    @Test
    void rejectsInvalidCapacityBaselineInputs() {
        assertThatThrownBy(() -> service.recordCapacityBaseline("order", 100000, 120000, 85000, 11, 10,
                new BigDecimal("0.72"), new BigDecimal("0.68"), new BigDecimal("12000"), true, Instant.now()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("current replicas");

        assertThatThrownBy(() -> service.recordCapacityBaseline("order", 100000, 120000, 85000, 9, 10,
                new BigDecimal("1.20"), new BigDecimal("0.68"), new BigDecimal("12000"), true, Instant.now()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("cpu utilization");
    }
}
